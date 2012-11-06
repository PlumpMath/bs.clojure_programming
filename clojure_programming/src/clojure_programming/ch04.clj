(ns ch04)

;; Shifting Computation Through Time and Space

;; Delays

(def d (delay (println "Running...")
              :done))

(deref d)                              ; @d

(def a-fn #(do
             (println "Running...")
             :done!))
(a-fn)


;;
(defn get-document
  [id]
  {:url "http://www.mozilla.org/about/manifesto.en.html"
   :title "The Mozilla Manifesto"
   :mime "text/html"
   :content (delay (slurp "http://www.mozilla.org/about/manifesto.en.html"))})

(def d (get-document "some-id"))

(realized? (:content d))                ; false
@(:content d)
(realized? (:content d))                ; true

;; Futures
(def long-calculation (future (apply + (range 1e8))))

@(future (Thread/sleep 5000) :done!)

(deref (future (Thread/sleep 5000) :done!)
  1000
  :impatient!)

;; Promises

(def p (promise))

(realized? p)

(deliver p 42)

(realized? p)
;; ch04> p
;; #<core$promise$reify__6153@27e28: 42>
;; ch04> @p
;; 42

(def a (promise))
(def b (promise))
(def c (promise))

(future
  (deliver c (+ @a @b))
  (println "Delivery @c complete"))

(deliver a 15)
(deliver b 16)

;; cyclic dependency
(def a (promise))
(def b (promise))
(future (deliver a @b))
(future (deliver b @a))
(realized? a)
(realized? b)

(deliver a 42)

;; callback
(defn call-service
  [arg1 arg2 callback-fn]
  (future (callback-fn (+ arg1 arg2) (- arg1 arg2))))

(defn sync-fn
  [async-fn]
  (fn [& args]
    (let [result (promise)]
      (apply async-fn (conj (vec args) #(deliver result %&)))
      @result)))

((sync-fn call-service) 8 7)

;; Parallelism on Cheap

(defn phone-numbers
  [string]
  (re-seq #"(\d{3})[\.-]?(\d{3})[\.-]?(\d{4})" string))

(phone-numbers "Sunil: 617.555.2937, Betty: 508.555.2218")
;; (["617.555.2937" "617" "555" "2937"] ["508.555.2218" "508" "555" "2218"])
(def files (repeat 100
                   (apply str
                          (concat (repeat 1000000 \space)
                                  "Sunil: 617.555.2937, Betty: 508.555.2218"))))

(time (dorun (map phone-numbers files)))
;; "Elapsed time: 1097.919051 msecs"
(time (dorun (pmap phone-numbers files)))
;; "Elapsed time: 665.950599 msecs"

