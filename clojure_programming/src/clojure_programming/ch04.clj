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


(def files (repeat 100000
                   (apply str
                          (concat (repeat 1000 \space)
                                  "Sunil: 617.555.2937, Betty: 508.555.2218"))))

(time (dorun (map phone-numbers files)))
;; "Elapsed time: 1161.865813 msecs"
(time (dorun (pmap phone-numbers files)))
;; "Elapsed time: 1045.375003 msecs"

(time (->> files
           (partition-all 250)
           (pmap (fn [chunk] (doall (map phone-numbers chunk))))
           (apply concat)
           dorun))
;; "Elapsed time: 647.853221 msecs"

;;;; Clojure Reference Types
;; vars, refs, agents, atoms

;; Atom : sync & uncoord
;; sync - function that change atom values don't return until they have completed
;; uncoord ??
(def sarah (atom {:name "Sarah" :age 25 :wears-glasses? false}))
(swap! sarah update-in [:age] + 3)
;; {:age 28, :wears-glasses? false, :name "Sarah"}
(swap! sarah (comp #(update-in % [:age] inc)
                   #(assoc % :wears-glasses? true)))
;; {:age 29, :wears-glasses? true, :name "Sarah"}

(def xs (atom #{1 2 3}))
(defmacro futures
  [n & exprs]
  (vec (for [_ (range n)
             expr exprs]
         `(future ~expr))))
(defmacro wait-futures
  [& args]
  `(doseq [f# (futures ~@args)]
     @f#))

(wait-futures 1
              (swap! xs (fn [v]
                          (Thread/sleep 250)
                          (println "trying 4")
                          (conj v 4)))
              (swap! xs (fn [v]
                          (Thread/sleep 500)
                          (println "trying 5")
                          (conj v 5))))
;; trying 4
;; trying 5
;; trying 5
;; nil

(def x (atom 2000))
(swap! x #(Thread/sleep %))

(compare-and-set! xs :wrong "new value")
(compare-and-set! xs @xs "new value")
(compare-and-set! xs "new value" 1)

;; Notifications and Constraints
;; all of clojure's reference type have hooks (whatches and validators)

;; Watch.
(defn echo-watch
  [key identity old new]
  (println key old "=>" new))
(def sarah (atom {:name "Sarah" :age 25}))
(add-watch sarah :echo echo-watch)
(swap! sarah update-in [:age] inc)
;; :echo {:age 25, :name Sarah} => {:age 26, :name Sarah}
(add-watch sarah :echo2 echo-watch)
(swap! sarah update-in [:age] inc)
;; :echo {:age 26, :name Sarah} => {:age 27, :name Sarah}
;; :echo2 {:age 26, :name Sarah} => {:age 27, :name Sarah}


(remove-watch sarah :echo2)
(swap! sarah update-in [:age] inc)
;; :echo {:age 27, :name Sarah} => {:age 28, :name Sarah}

(reset! sarah @sarah)
;; :echo {:age 28, :name Sarah} => {:age 28, :name Sarah}

(def history (atom ()))

(defn log->list
  [dest-atom key source old new]
  (when (not= old new)
    (swap! dest-atom conj new)))

(def sarah (atom {:name "Sarah", :age 25}))
(add-watch sarah :record (partial log->list history))
(swap! sarah update-in [:age] inc)
(swap! sarah update-in [:age] inc)
(swap! sarah identity)
(swap! sarah assoc :wears-glasses? true)
(swap! sarah update-in [:age] inc)
(pprint @history)

;; Validators
