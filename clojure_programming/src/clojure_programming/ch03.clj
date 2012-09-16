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

(def a
  ^{:created (System/currentTimeMillis)}
  [1 2 3])
(meta a)
;; {:created 1347290765294}

;; (let [millis (System/currentTimeMillis)]
;;   (java.util.concurrent.TimeUnit/MILLISECONDS millis))


;;;; Thinking Different

(defn empty-board
  [w h]
  (vec (repeat w (vec (repeat h nil)))))

(empty-board 10 20)

(defn populate
  [board living-cells]
  (reduce (fn [board coords]
            (assoc-in board coords :on))
          board
          living-cells))

(def glider
  (populate (empty-board 6 6)
            #{[2 0] [2 1] [2 2] [1 2] [0 1]}))

(pprint glider)

(defn neighbours
  [[x y]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not= 0 dx dy)]
    [(+ dx x) (+ dy y)]))

(defn count-neighours
  [board loc]
  (count (filter #(get-in board %) (neighbours loc))))

(defn indexed-step
  [board]
  (let [w (count board)
        h (count (first board))]
    (loop [new-board board x 0 y 0]
      (cond (>= x w) new-board
            (>= y h) (recur new-board (inc x) 0)
            :else    (let [new-liveness
                           (case (count-neighours board [x y])
                             2 (get-in board [x y])
                             3 :on
                             nil)]
                       (recur (assoc-in new-board [x y]
                                        new-liveness)
                              x
                              (inc y)))))))

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