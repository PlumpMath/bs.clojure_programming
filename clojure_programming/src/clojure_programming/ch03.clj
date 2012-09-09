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
(get-bar nil)                           ; NullPointerException

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

(time (do (into #{} (range 1e8))
          nil))




