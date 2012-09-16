(ns ch03)


;;;; Sequences

(seq "hello")                           ; (\h \e \l \l \o)
(seq [1 2 3])                           ; (1 2 3)
(seq {:a 1 :b 2})                       ; ([:a 1] [:b 2])
(seq [])                                ; nil
(seq nil)                               ; nil

(let [s (range 1e6)]
  (time (count s)))

;;;; Lazy seqs

(defn random-ints
  [limit]
  (lazy-seq
   (cons (rand-int limit)
         (random-ints limit))))

(take 10 (random-ints 50))

;;;; Head retention
(split-with neg? (range -5 5))
;; [(-5 -4 -3 -2 -1) (0 1 2 3 4)]

;;; Sorted
(defn interpolate
  [points]
  (let [results (into (sorted-map) (map vec points))]
    (fn [x]
      (let [[x1 y1] (first (rsubseq results <= x))
            [x2 y2] (first (subseq results > x))]
        (if (and x1 x2)
          (/ (+ (* y1 (- x2 x))
                (* y2 (- x x1)))
             (- x2 x1))
          (or y1 y2))))))

(def f (interpolate [[0 0] [10 10] [15 5]]))

(map f [2 10 12])
;; (2 10 8)

;;;; Idiomatic Usage
(defn get-foo
  [map]
  (:foo map))

(defn get-bar
  [map]
  (map :bar))

(get-foo nil)                           ; nil
;; (get-bar nil)                           ; NullPointerException

;;;; Beware of the nil(again)
(remove #{5 7} (cons false (range 10)))
;; (false 0 1 2 3 4 6 8 9)

(remove #{false 5 7} (cons false (range 10)))
;; (false 0 1 2 3 4 6 8 9)

(remove (partial contains? #{5 7 false})
        (cons false (range 10)))
;; (0 1 2 3 4 6 8 9)

;;;; Other usage of maps
(group-by #(rem % 3) (range 10))
;; {0 [0 3 6 9], 1 [1 4 7], 2 [2 5 8]}

;;;; Transients
(def x (transient []))
(def y (conj! x 1))
(count x)                               ; 1
(count y)                               ; 1

(defn naive-into
  [coll source]
  (reduce conj coll source))

(defn faster-into
  [coll source]
  (persistent! (reduce conj! (transient coll) source)))

(time (do (into #{} (range 1e6))
          nil))
;; "Elapsed time: 227.952806 msecs"
(time (do (naive-into #{} (range 1e6))
          nil))
;; "Elapsed time: 437.37291 msecs"
(time (do (faster-into #{} (range 1e6))
          nil))
;; "Elapsed time: 276.599278 msecs"

(= (transient [1 2])
   (transient [1 2]))
;; false

;;;; MetaData

(meta ^:private [1 2 3])
;; {:private true}
(meta ^:private ^:dynamic [1 2 3])
;; {:dynamic true, :private true}

(def a
  ^{:created (System/currentTimeMillis)}
  [1 2 3])
(meta a)
;; {:created 1347290765294}

(def b (with-meta a (assoc (meta a)
                      :modified (System/currentTimeMillis))))
(meta b)
;; {:modified 1347761532079, :created 1347761526881}

(def c (vary-meta a assoc :modified (System/currentTimeMillis)))
(meta c)
;; {:modified 1347761612033, :created 1347761526881}

;;;; Thinking Different
;;; Revisiting a classic: Consway's Game of Life

(defn empty-board
  ;; (empty-board 2 3)
  ;; [[nil nil] [nil nil] [nil nil]]
  [w h]
  (vec (repeat h (vec (repeat w nil)))))


(defn populate
  ;; (populate (empty-board 2 3) #{[0 1]})
  ;; [[nil :on] [nil nil] [nil nil]]
  [board living-cells]
  (reduce (fn [board coords]
            (assoc-in board coords :on))
          board
          living-cells))

(defn neighbours
  ;; (neighbours [0 0])
  ;; ([-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1])
  [[x y]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not= 0 dx dy)]
    [(+ dx x) (+ dy y)]))

(defn count-neighours
  [board loc]
  (count (filter #(get-in board %) (neighbours loc))))

(count-neighours
 (populate (empty-board 2 3) #{[0 0] [0 1] [1 1]})		       [0 0])
;; 2

(defn indexed-step
  [board]
  (let [w (count (first board))
        h (count board)]
    (loop [new-board board x 0 y 0]
      (cond (>= y h) new-board
            (>= x w) (recur new-board 0 (inc y))
            :else    (let [new-liveness
                           (case (count-neighours board [x y])
                             2 (get-in board [x y])
                             3 :on
                             nil)]
                       (recur (assoc-in new-board [x y]
                                        new-liveness)
                              (inc x)
                              y))))))

(def glider
  (populate (empty-board 6 6)
            #{[2 0] [2 1] [2 2] [1 2] [0 1]}))

(pprint glider)

(-> (iterate indexed-step glider)
    (nth 1)
    pprint)




(defn indexed-step-2
  [board]
  (let [w (count board)
        h (count (first board))]
    (reduce (fn [new-board x]
              (reduce
               (fn [new-board y]
                 (let [new-liveness
                       (case (count-neighours board [x y])
                         2 (get-in board [x y])
                         3 :on
                         nil)]
                   (assoc-in new-board [x y] new-liveness)))
               new-board (range h)))
            board (range w))))

(-> (iterate indexed-step-2 glider)
    (nth 7)
    pprint)

(defn indexed-step-3
  [board]
  (let [w (count board)
        h (count (first board))]
    (reduce (fn [new-board [x y]]
              (let [new-liveness
                    (case (count-neighours board [x y])
                      2 (get-in board [x y])
                      3 :on
                      nil)]
                   (assoc-in new-board [x y] new-liveness)))
            board (for [x (range h)
                        y (range w)]
                    [x y]))))


(for [i (range 1 8)]
  (-> (iterate indexed-step-2 glider)
      (nth i)
      pprint))


(partition 3 1 (range 5))
;; ((0 1 2) (1 2 3) (2 3 4))
(partition 3 2 (range 5))
;; ((0 1 2) (2 3 4))

;; (defn window
;;    [coll]
;;    (partition 3 1
;;               (concat [nil]
;;                       coll
;;                       [nil])))

;; (defn cell-block
;;   [[left mid right]]
;;   (window (map vector
;;                (or left (repeat nil))
;;                mid
;;                (or right (repeat nil)))))

(defn window
  ;; (window [nil :on nil])
  ;; => ((nil nil :on) (nil :on nil) (:on nil nil))
  ([coll] (window nil coll))
  ([pad coll]
     (partition 3 1 (concat [pad] coll [pad]))))

(defn cell-block
  ;; (cell-block (window [nil :on nil]))
  ;; = > ((nil [nil nil :on] [nil :on nil]) ([nil nil :on] [nil :on nil] [:on nil nil]) ([nil :on nil] [:on nil nil] nil))
  [[left mid right]]
  (window (map vector left mid right)))

(defn liveness
  [block]
  (let [[_ [_ center _] _] block]
    (case (- (count (filter #{:on} (apply concat block)))
             (if (= :on center) 1 0))
      2 center
      3 :on
      nil)))

(defn- step-row
  [rows-triple]
  
  (vec (map liveness (cell-block rows-triple))))

(defn index-free-step
  [board]
  (vec (map (step-row (window (repeat nil) board)))))(frequencies [1 2 2 2 2 3])
{1 1, 2 4, 3 1}

;; (= (nth (iterate indexed-step glider) 8)
;;     (nth (iterate index-free-step glider) 8))

(nth (iterate indexed-step glider) 8)
;; (nth (iterate index-free-step glider) 8)

(frequencies [1 2 2 2 2 3])
;; {1 1, 2 4, 3 1}

;;; Maze generation
;; http://weblog.jamisbuck.org/2011/2/7/maze-generation-algorithm-recap
(defn maze
  [walls]
  (let [paths (reduce (fn [index [a b]]
                        (merge-with into index {a [b] b [a]}))
                      {}
                      (map seq walls))
        start-loc (rand-nth (keys paths))]
    (loop [walls walls
           unvisited (disj (set (keys paths)) start-loc)]
      (if-let [loc (when-let [s (seq unvisited)]
                     (rand-nth s))]
        (let [walk (iterate (comp rand-nth paths) loc)
              steps (zipmap (take-while unvisited walk) (next walk))]
          (recur (reduce disj walls (map set steps))
                 (reduce disj unvisited (keys steps))))
        walls))))

(defn grid
  [w h]
  (set (concat
        (for [i (range (dec w))
              j (range h)]
          #{[i j] [(inc i) j]})
        (for [i (range w)
              j (range (dec h))]
          #{[i j] [i (inc j)]}))))

(defn draw
  [h w maze]
  (doto (javax.swing.JFrame. "Maze")
    (.setContentPane
     (doto (proxy [javax.swing.JPanel] []
             (paintComponent [^java.awt.Graphics g]
               (let [g (doto ^java.awt.Graphics2D (.create g)
                             (.scale 10 10)
                             (.translate 1.5 1.5)
                             (.setStroke (java.awt.BasicStroke. 0.4)))]
                     (.drawRect g -1 -1 w h)
                     (doseq [[[x1 y1] [x2 y2]] (map sort maze)]
                       (let [[x y] (if (= x1 x2)
                                       [(dec x1) y1]
                                       [x1 (dec y1)])]
                         (.drawLine g x1 y1 x y))))))
       (.setPreferredSize (java.awt.Dimension.
                           (* 10 (inc w)) (* 10 (inc h))))))
    .pack
    (.setVisible true)))

(draw 40 40 (maze (grid 40 40)))