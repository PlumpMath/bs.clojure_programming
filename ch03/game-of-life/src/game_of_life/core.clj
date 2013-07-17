(ns game-of-life.core)
(use 'clojure.pprint)

(defn empty-board  [w h]
  ;; (empty-board 2 3)
  ;; => [[nil nil] [nil nil] [nil nil]]
  (vec (repeat h (vec (repeat w nil)))))


(defn populate [board living-cells]
  ;; (populate (empty-board 2 3) #{[0 1]})
  ;; => [[nil :on] [nil nil] [nil nil]]
  (reduce (fn [board coords]
            (assoc-in board coords :on))
          board
          living-cells))

(defn neighbours [[x y]]
  ;; (neighbours [0 0])
  ;; => ([-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1])
  (for [dx [-1 0 1] dy [-1 0 1] :when (not= 0 dx dy)]
    [(+ x dx) (+ y dy)]))

(defn count-neighours [board loc]
  ;; (count-neighours
  ;;  (populate (empty-board 2 3) #{[0 0] [0 1] [1 1]})
  ;;  [0 0])
  ;; => 2
  (count (filter #(get-in board %) (neighbours loc))))

;; ============
;; indexed step
(defn indexed-step [board]
  (let [w (count (first board))
        h (count board)]
    (loop [new-board board x 0 y 0]
      (cond (>= y h) new-board
            (>= x w) (recur new-board 0 (inc y))
            :else
            (let [new-liveness
                  (case (count-neighours board [x y])
                    2 (get-in board [x y])
                    3 :on
                    nil)]
              (recur (assoc-in new-board [x y] new-liveness)
                     (inc x) y))))))

(defn indexed-step-2 [board]
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

(defn indexed-step-3 [board]
  (let [w (count board)
        h (count (first board))]
    (reduce (fn [new-board [x y]]
              (let [new-liveness
                    (case (count-neighours board [x y])
                      2 (get-in board [x y])
                      3 :on
                      nil)]
                (assoc-in new-board [x y] new-liveness)))
            board (for [x (range h) y (range w)]
                    [x y]))))

;; free step
(defn window
  ;; (window [nil :on nil])
  ;; => ((nil nil :on) (nil :on nil) (:on nil nil))
  ([coll] (window nil coll))
  ([pad coll]
     (partition 3 1 (concat [pad] coll [pad]))))

(defn cell-block [[left mid right]]
  ;; (cell-block (window [nil :on nil]))
  ;; =>
  ;; ((nil           [nil nil :on] [nil :on nil])
  ;;  ([nil nil :on] [nil :on nil] [:on nil nil])
  ;;  ([nil :on nil] [:on nil nil] nil          ))
  (window (map vector left mid right)))

(defn liveness [block]
  (let [[_ [_ center _] _] block
        delta (if (= :on center) 1 0)
        on-cnt (count (filter #{:on} (apply concat block)))]
    (case (- on-cnt delta)
      2 center
      3 :on
      nil)))

(defn- step-row [rows-triple]
  (vec (map liveness (cell-block rows-triple))))

(defn index-free-step [board]
  (vec (map step-row (window (repeat nil) board))))

;; =========================================
;; test steps

(def glider
  (populate (empty-board 6 6)
            #{[2 0] [2 1] [2 2] [1 2] [0 1]}))

;; (pprint glider)
;;>>
;; [[nil :on nil nil nil nil]
;;  [nil nil :on nil nil nil]
;;  [:on :on :on nil nil nil]
;;  [nil nil nil nil nil nil]
;;  [nil nil nil nil nil nil]
;;  [nil nil nil nil nil nil]]


;; (-> (iterate indexed-step glider)
;;     (nth 1)
;;     pprint)

;; (-> (iterate indexed-step-2 glider)
;;     (nth 7)
;;     pprint)

;; (-> (iterate indexed-step-3 glider)
;;     (nth 7)
;;     pprint)


;; (-> (iterate index-free-step glider)
;;     (nth 8)
;;     pprint)

;; (= (nth (iterate indexed-step glider) 8)
;;     (nth (iterate index-free-step glider) 8))

;; =========
;; Main
(defn step [cells]
  (set (for [[loc n] (frequencies (mapcat neighbours cells))
             :when (or (= n 3) (and (= n 2) (cells loc)))]
         loc)))

(->> (iterate step #{[2 0] [2 1] [2 2] [1 2] [0 1]})
     (drop 8)
     first
     (populate (empty-board 6 6))
     pprint)

;; ===
;; hex
(defn stepper
  [neighbours birth? survive?]
  (fn [cells]
    (set (for [[loc n] (frequencies (mapcat neighbours cells))
               :when (if (cells loc) (survive? n) (birth? n))]
           loc))))

(defn hex-neighbours [[x y]]
  (for [dx [-1 0 1]
        dy (if (zero? dx) [-2 2] [-1 1])]
    [(+ x dx) (+ y dy)]))

(def hex-step (stepper hex-neighbours #{2} #{3 4}))

(hex-step #{[0 0] [1 1] [1 3] [0 4]})


;; =====================
;; GUI
(import '(java.awt Color Dimension)
        '(javax.swing JPanel JFrame Timer)
        '(java.awt.event ActionListener))

(def glider-info #{[2 0] [2 1] [2 2] [1 2] [0 1]})
(def lightweight-spaceship #{[0 1] [0 4]
                             [1 0]
                             [2 0] [2 4]
                             [3 0] [3 1] [3 2] [3 3]
                             })

(defn adjust-pos [cells [dx dy]]
  (set (map (fn [[x y]] [(+ x dx) (+ y dy)]) cells)))

(def atom-cell (atom lightweight-spaceship))
(def atom-cell (atom glider-info))

(defn adjust1 [x a-max]
  (let [x (rem x a-max)
        x (if (< x 0) (+ x a-max) x)]
    x))

(defn paint [^java.awt.Graphics g]
  (. g setColor Color/RED)
  (let [sz 10]
    (swap! atom-cell step)
    (doseq [[x y] @atom-cell]
      (let [x (adjust1 x 30)
            y (adjust1 y 30)]
        (. g fillRect (* x sz) (* y sz) sz sz)))))

(defn new-game-panel []
  (proxy [JPanel ActionListener] []
    (paintComponent [g]
      (proxy-super paintComponent g)
      (paint g))
    (actionPerformed [e]
      (.repaint this))))

(defn gui-game-of-life []
  (let [frame (JFrame. "Game Of Life")
        panel (new-game-panel)
        timer (Timer. 75 panel)]
    (. timer start)
    (. panel setPreferredSize (Dimension. 350 350))

    (doto frame
      (.add panel)
      (.pack)
      (.setVisible true))))
