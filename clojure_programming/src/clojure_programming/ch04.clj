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
(def n (atom 1 :validator pos?))
;; (def n (atom -1 :validator pos?)) => error
(swap! n + 500)
(swap! n - 1000)

(def sarah (atom {:name "Sarah" :age 25}))
(set-validator! sarah :age)
;; (swap! sarah dissoc :age) => error

(set-validator! sarah #(or (:age %)
                           (throw (IllegalStateException. "People must have `:age`s!"))))
;; (swap! sarah dissoc :age) => error "People must have `:age`s!"

;;; Refs - sync && coord
(defn test-func
  [& {:as args}]
  (println args))
(test-func 1 2 3 4)
;; {1 2, 3 4}

(defn character
  [name & {:as opts}]
  (ref (merge {:name name :items #{} :health 500}
              opts)))

(def smaug (character "Smaug" :health 500 :strength 400 :items (set (range 50))))
(def bilbo (character "Bilbo" :health 100 :strength 100))
(def gandalf (character "Gandalf" :health 75 :mana 750))

(defn loot
  [from to]
  (dosync
   (when-let [item (first (:items @from))]
     (alter to update-in [:items] conj item)
     (alter from update-in [:items] disj item))))

(wait-futures 1
              (while (loot smaug bilbo))
              (while (loot smaug gandalf)))

(map (comp count :items deref) [bilbo gandalf])
;; (33 17)
(filter (:items @bilbo) (:items @gandalf))
;; ()

;; understanding alter
;; :more https://en.wikipedia.org/wiki/Serializability
;; Mimimizing transaction conflict with commute
;; :more https://en.wikipedia.org/wiki/Commutative_property

(= (/ (/ 120 3) 4)
   (/ (/ 120 4) 3))                     ; true
(= ((comp #(/ % 3) #(/ % 4)) 120)
   ((comp #(/ % 4) #(/ % 3)) 120))      ; true


(def x (ref 0))
(time (wait-futures 5
                    (dotimes [_ 1000]
                      (dosync (alter x + (apply + (range 1000)))))
                    (dotimes [_ 1000]
                      (dosync (alter x - (apply + (range 1000)))))))
;; "Elapsed time: 613.368608 msecs"

(time (wait-futures 5
                    (dotimes [_ 1000]
                      (dosync (commute x + (apply + (range 1000)))))
                    (dotimes [_ 1000]
                      (dosync (commute x - (apply + (range 1000)))))))
;; "Elapsed time: 302.520454 msecs"

(defn flawed-loot
  [from to]
  (dosync
   (when-let [item (first (:items @from))]
     (commute to update-in [:items] conj item)
     (commute from update-in [:items] disj item))))

(def smaug (character "Smaug" :health 500 :strength 400 :items (set (range 50))))
(def bilbo (character "Bilbo" :health 100 :strength 100))
(def gandalf (character "Gandalf" :health 75 :mana 750))

(wait-futures 1
              (while (flawed-loot smaug bilbo))
              (while (flawed-loot smaug gandalf)))

;; ch04> (map (comp count :items deref) [bilbo gandalf])
;; (18 48)
;; ch04> (filter (:items @bilbo) (:items @gandalf))
;; (0 32 1 33 2 34 3 35 4 36 5 37 6 38 7 40)
;; ch04> (count (filter (:items @bilbo) (:items @gandalf)))
;; 16

(defn fixed-loot
  [from to]
  (dosync
   (when-let [item (first (:items @from))]
     (commute to update-in [:items] conj item)
     (alter from update-in [:i(wait-futures 1
              (play bilbo attack smaug)
              (play smaug attack bilbo))
tems] disj item))))


(def smaug (character "Smaug" :health 500 :strength 400 :items (set (range 50))))
(def bilbo (character "Bilbo" :health 100 :strength 100))
(def gandalf (character "Gandalf" :health 75 :mana 750))

(wait-futures 1
              (while (fixed-loot smaug bilbo))
              (while (fixed-loot smaug gandalf)))
;; ch04> (map (comp count :items deref) [bilbo gandalf])
;; (49 1)
;; ch04> (filter (:items @bilbo) (:items @gandalf))
;; ()

(defn attack
  [aggressor target]
  (dosync
   (let [damage (* (rand 0.1) (:strength @aggressor))]
     (commute target update-in [:health] #(max 0 (- % damage))))))

(defn heal
  [healer target]
  (dosync
   (let [aid (* (rand 0.1) (:mana @healer))]
     (when (pos? aid)
       (commute healer update-in [:mana] - (max 5 (/ aid 5)))
       (commute target update-in [:health] + aid)))))

(def alive? (comp pos? :health))

(defn play
  [character action other]
  (while (and (alive? @character)
              (alive? @other)
              (action character other))
    (Thread/sleep (rand-int 50))))

(wait-futures 1
              (play bilbo attack smaug)
              (play smaug attack bilbo))
(map (comp :health deref) [smaug bilbo])

(dosync
 (alter smaug assoc :health 500)
 (alter bilbo assoc :health 100))

(wait-futures 1
              (play bilbo attack smaug)
              (play smaug attack bilbo)
              (play gandalf heal bilbo))

(map (comp #(select-keys % [:name :health :mana]) deref) [smaug bilbo gandalf])

;; ({:health 0, :name "Smaug"} {:health 336.6878640864034, :name "Bilbo"} {:mana -3.088366726146262, :health 75, :name "Gandalf"})

(dosync (ref-set bilbo {:name "Bilbo"}))
(dosync (alter bilbo (constantly {:name "Bilbo"})))


(defn- enforce-max-health
;;  [{:keys [name health]}]
  [name health]
  (fn [character-data]
    (or (<= (:health character-data) health)
        (throw (IllegalStateException. (str name " is already at max health!"))))))

(defn character
  [name & {:as opts}]
  (let [cdata (merge {:name name :items #{} :health 500}
                     opts)
        cdata (assoc cdata :max-health (:health cdata))
        validators (list* (enforce-max-health name (:health cdata))
                          (:validators cdata))]
    (ref (dissoc cdata :validators)
         :validator #(every? (fn [v] (v %)) validators))))

(def bilbo (character "Bilbo" :health 100 :strength 100))

(defn test-func
  [{:keys [a b]}]
  (prn a b))

(test-func '(:a 1))                     ;=> 1 nil
(test-func '(:a 1 :b 2))                ;=> 1 2

(heal gandalf bilbo)

(dosync (alter bilbo assoc-in [:health] 95))
;; (heal gandalf bilbo) => error

(defn heal
  [healer target]
  (dosync
   (let [aid (min (* (rand 0.1) (:mana @healer))
                  (- (:max-health @target) (:health @target)))]
     (when (pos? aid)
       (commute healer update-in [:mana] - (max 5 (/ aid 5)))
       (alter target update-in [:health] + aid)))))

(heal gandalf bilbo)

;; Side-effecting functions strictly verboten
(def x (ref (java.util.ArrayList.)))
(wait-futures 2
              (dosync (dotimes [v 5]
                        (Thread/sleep (rand-int 50))
                        (alter x #(doto % (.add v))))))
;; @x => #<ArrayList [0, 0, 1, 2, 0, 3, 4, 0, 1, 2, 3, 4]>

(def x (ref 10))                  ; @x => 10
;; (dosync
;;  @(future (dosync (ref-set x 0)))
;;  (ref-set x 1))
;; => error
;; @x => 0

;;; Readers may retry
;; enough minimum history to a good value.
(ref-min-history (ref "abc" :min-history 3 :max-history 30))   ; => 3
(ref-max-history (ref "abc" :min-history 3 :max-history 30))   ; => 30
(ref-history-count (ref "abc" :min-history 3 :max-history 30)) ; => 0
;; --
(def a (ref 0))
(future (dotimes [_ 500]
          (dosync (Thread/sleep 200)
                  (alter a inc))))
@(future (dosync (Thread/sleep 1000) @a))
(ref-history-count a)
;; --
(def a (ref 0 :max-history 100))
(future (dotimes [_ 500]
          (dosync (Thread/sleep 20)
                  (alter a inc))))
@(future (dosync (Thread/sleep 1000) @a))
(ref-history-count a)
;; --
(def a (ref 0 :min-history 50 :max-history 100))
(future (dotimes [_ 500]
          (dosync (Thread/sleep 20)
                  (alter a inc))))
@(future (dosync (Thread/sleep 1000) @a))
(ref-history-count a)


;;; Write skew
;; :more http://en.wikipedia.org/wiki/Snapshot_isolation
(def daylight (ref 1))
(defn attack
  [aggressor target]
  (dosync
;;   (let [damage (* (rand 0.1) (:strength @aggressor) @daylight)] ; skey
   (let [damage (* (rand 0.1) (:strength @aggressor) (ensure daylight))] ; daylight will not change before the reading transaction commits success.
     (commute target update-in [:health] #(max 0 (- % damage))))))

;;; Vars
;; ch04> map
;; #<core$map clojure.core$map@4e5acdf>
;; ch04> #'map
;; #'clojure.core/map
;; ch04> @#'map
;; #<core$map clojure.core$map@4e5acdf>
;; ch04> `~map
;; #<core$map clojure.core$map@4e5acdf>
(def ^:private everything 42)
(def ^{:private true} evrything 42)
(ns other-namespace)
(refer 'ch04)
;; everything         => error: Unable to resolve symbol
;; ch04/everything    => error: is not public
;; @#'ch04/everything => 42

(def a
  "A sample value"
  5)
(defn b
  "A simple calculation using `a`."
  [c]
  (+ a c))

(doc a)
(doc b)
(meta #'a)
(meta #'b)

;; ^:const - value captured by function at compile-time.
(def ^:const max-value 255)
(defn valid-value?
  [v]
  (<= v max-value))
(valid-value? 299)                      ; flase
(def max-value 500)
(valid-value? 299)                      ; flase

;; Dynamic Scope
(def ^:dynamic *max-value* 255)
(defn valid-value?
  [v]
  (<= v *max-value*))
(binding [*max-value* 500]
  ;; binding(Macro): The new bindings are made in parallel (unlike let)
  (println (valid-value? 299))
  (doto (Thread. #(println "in other thread: " (valid-value? 299)))
    .start
    .join))

;; Expaneded
;; (let* []
;;       (clojure.core/push-thread-bindings
;;        (clojure.core/hash-map (var *max-value*) 500))
;;       (try
;;         (println (valid-value? 299))
;;         (doto (Thread. (fn* [] (println "in other thread: " (valid-value? 299))))
;;           .start
;;           .join)
;;         (finally
;;           (clojure.core/pop-thread-bindings))))

(def ^:dynamic *var* :root)
(defn get-*var* [] *var*)
(let [*var* :a]
  (println "*var*      : " *var*)        ; :a
  (println "(get-*var*): " (get-*var*))) ; :root

(binding [*var* :a]
  (println "*var*      : " *var*)        ; :a
  (println "(get-*var*): " (get-*var*))) ; :a

(defn http-get
  [url-string]
  (let [conn (-> url-string java.net.URL. .openConnection)
        response-code (.getResponseCode conn)]
    (if (== 404 response-code)
      [response-code]
      [response-code (-> conn .getInputStream slurp)])))

(http-get "http://google.com/bad-url")
(http-get "http://google.com/")


(def ^:dynamic *response-code* nil)
(defn http-get
  [url-string]
  (let [conn (-> url-string java.net.URL. .openConnection)
        response-code (.getResponseCode conn)]
    (when (thread-bound? #'*response-code*)
      (set! *response-code* response-code))
    (when (not= 404 response-code)
      (-> conn .getInputStream slurp))))
    ;; (if (== 404 response-code)
    ;;   [response-code]
    ;;   [response-code (-> conn .getInputStream slurp)])))

(http-get "http://google.com")
(do
  (binding [*response-code* nil]
    (let [content (http-get "http://google.com/bad-url")]
      (println "Response code was: " *response-code*)))
  *response-code*)

;; Dynamic scope propagates through Clojure-native concurrency forms.
(binding [*max-value* 500]
  (println (valid-value? 299))
  @(future (valid-value? 299)))

(binding [*max-value* 500]
  (map valid-value? [299]))             ; (false)
(binding [*max-value* 500]
  (pmap valid-value? [299]))            ; (true)
(map #(binding [*max-value* 500]
        (valid-value? %)) [299])        ; (true)


;; Changing a var's Root Binding.
(def x 0)
(alter-var-root #'x inc)                ; x => 1
;; (alter-var-root x inc) ; error: java.lang.ClassCastException: java.lang.Long cannot be cast to clojure.lang.Var

(do
  (with-redefs [*max-value* 500]
    (println *max-value*))              ; 500
  (println *max-value*))                ; 255

;; Forward Declarations
(def something-var)
(declare something-fn)

;;; :TODO *** Agents - async & uncoord
;; Actions(queued using 'send')
;; - evaluated within a fixed-size thread pool
;; - :TODO ??? don't I/O or other blocking op
;; Actions(queued using 'send-off')
;; - evaluated within an unbounded thread pool
(def a (agent 500))
(send a range 1000)

(def a (agent 0))
(send a inc)

(def a (agent 5000))
(def b (agent 10000))
(send-off a #(Thread/sleep %))
(send-off b #(Thread/sleep %))
(await a b)

;; Dealing with Errors in Agent Actions
(def a (agent nil))
(send a (fn [_]
          (throw (Exception. "something is wrong"))))
;; (send a identity) ; error: java.lang.RuntimeException: Agent is failed, needs restart
(restart-agent a 42)
(send a inc)
(reduce send a (for [x (range 3)]
                 (fn [_]
                   (throw (Exception. (str "error #" x))))))
(agent-error a)                         ; #<Exception java.lang.Exception: error #0>
(restart-agent a 42)
(agent-error a)                         ; #<Exception java.lang.Exception: error #1>
(restart-agent a 42 :clear-actions true)
(agent-error a)                         ; nil

;; -------------------------
;; clojure.core/agent-error
;; ([a])
;;   Returns the exception thrown during an asynchronous action of the
;;   agent if the agent is failed.  Returns nil if the agent is not
;;   failed.


;; Agent error handler and modes
(def a (agent nil :error-mode :continue))
(send a (fn [_]
          (throw (Exception. "something is wrong"))))
(send a identity)

(def a (agent nil
              :error-mode :continue
              :error-handler (fn [the-agent exception]
                               (println (.getMessage exception)))))
(send a (fn [_]
          (throw (Exception. "something is wrong"))))
(send a identity)

(set-error-handler! a (fn [the-agent exception]
                        (when (= "FATAL" (.getMessage exception))
                          (set-error-mode! the-agent :fail))))
(send a (fn [_]
          (throw (Exception. "FATAL"))))
;; (send a identity) ; Exception FATAL  ch04/eval1599/fn--1600 (NO_SOURCE_FILE:1)

;;
;; Persisting reference states with an agent-based write-behind log
;;
(require '[clojure.java.io :as io])

(def console (agent *out*))
(def character-log
  (agent (io/writer "character-states.log" :append true)))

(defn write
  [^java.io.Writer w & content]
  (doseq [x (interpose " " content)]
    (.write w (str x)))
  (doto w
    (.write "\n")
    .flush))

(defn log-referece
  [reference & writer-agents]
  (add-watch reference :log
             (fn [_ reference old new]
               (doseq [writer-agent writer-agents]
                 (send-off writer-agent write new)))))

(def smaug
  (character "Smaug" :health 500 :strength 400))
(def bilbo
  (character "Bilbo" :health 100 :strength 100))
(def gandalf
  (character "Gandalf" :health 75 :mana 1000))

(log-referece bilbo console character-log)
(log-referece smaug console character-log)

(wait-futures 1
              (play bilbo attack smaug)
              (play smaug attack bilbo)
              (play gandalf heal bilbo))

(defn attack
  [aggressor target]
  (dosync
   (let [damage (* (rand 0.1)
                   (:strength @aggressor)
                   (ensure daylight))]
     (send-off console write
               (:name @aggressor) "hits" (:name @target) "for" damage)
     (commute target update-in [:health] #(max 0 (- % damage))))))

(defn heal
  [healer target]
  (dosync
   (let [aid (min (* (rand 0.1) (:mana @healer))
                  (- (:max-health @target) (:health @target)))]
     (when (pos? aid)
       (send-off console write
                 (:name @healer) "heals" (:name @target) "for" aid)
       (commute healer update-in [:mana] - (max 5 (/ aid 5)))
       (alter target update-in [:health] + aid)))))

(dosync
 (alter smaug assoc :healthth 500)
 (alter bilbo assoc :health 100))

(wait-futures 1
              (play bilbo attack smaug)
              (play smaug attack bilbo)
              (play gandalf heal bilbo))

;;
;; Using agents to parallelize workloads
;;
;; https://github.com/cgrand/enlive/wiki
;; If you use Leiningen, add [enlive "1.0.0"] to your project.clj dependencies. (This won’t work with Clojure 1.0.)
(require '[net.cgrand.enlive-html :as enlive])