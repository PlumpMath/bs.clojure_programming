Concurrency and Parallelism
==========
* STM?
 - 소프트웨어 트랜잭셔널 메모리(Software Transactional Memory, STM)는 컴퓨터 과학에서 병행 컴퓨팅에서 공유 메모리으로의 접근을 제어하기 위한 데이터베이스 트랜잭션과 유사한 동시성 제어 구조이다 - http://ko.wikipedia.org/wiki/소프트웨어_트랜잭셔널_메모리
 - 클로져는 STM을 기본으로 제공함.

## delay
```clojure
(def d (delay (println "Running...")
              :done))

(realized? d) ;=> false

(deref d) ; same as @d
;>> Running...
;=> :done!

(realized? d) ;=> true

(deref d)
;=> :done!

(realized? d) ;=> true
```

코드가 한번만 평가됨. 캐쉬된걸 사용.

## Futures

```clojure
(def long-calculation
  (future
    (let [x (apply + (range 1e8))]
      (println "Done")
      x)))

long-calculation
;=> #<core$future_call$reify__6267@32737d8a: :pending>

@long-calculation
;>> Done
;=> 4999999950000000

long-calculation
;=> #<core$future_call$reify__6267@32737d8a: :4999999950000000>
```

```clojure
(def a (promise))
;=> #'test/a
(def b (promise))
;=> #'test/b
(def c (promise))
;=> #'test/c

(future
  (deliver c (+ @a @b))
  (println "Delivery complete!"))
;=> #<core$future_call$reify__6267@223f066a: :pending>

(deliver a 15)
;=> #<core$promise$reify__6310@7ce1727b: 15>
(deliver b 16)
;=> #<core$promise$reify__6310@3f9ac6e6: 16>
(deref c)
;=> 31
```


# Clojure Reference Types
* coordinate
 - 여러개의 actor들이 같이 작업하여, 올바른 결과를 낼 수 있는것.
* uncoordinate
 - 여러개의 actor들이 서로에 대해 영향을 줄 수 없는것.
* synchronous
 - 주어진 context에 대한 독점적(exclusive)으로 접근하는 동안, 쓰래드의 동작을 wait, block, sleep시키는것.
* asynchronous
 - 쓰레드의 동작에 대해 간섭하지 않고, 연산을 수행할 수 있는 것.

## atoms (uncoordinated, synchronous)
```clojure
(def atom-a (atom {:x 10}))             ;=> #'test/atom-a

(swap! atom-a update-in [:x] + 10)      ;=> {:x 20}
(deref atom-a)                          ;=> {:x 20}

(update-in @atom-a [:x] + 20)           ;=> {:x 40}
(deref atom-a)                          ;=> {:x 40}


(def atom-b (atom 10))                      ;=> #'test/atom-b

(compare-and-set! atom-b 99 "new val")      ;=> false
(deref atom-b)                              ;=> 10

(compare-and-set! atom-b @atom-b "new val") ;=> true
(deref atom-b)                              ;=> "new val"

(reset! atom-b "new new val")               ;=> "new new val"
(deref atom-b)                              ;=> "new new val"
```

## refs (coordinated, synchronous)
* No possibility of the involved refs ever being in an observable inconsistent state
* No possibility of race conditions among the involved refs
* No manual use of locks, monitors, or oter low-level synchronization primitives
* No possibility of deadlocks

클로져의 STM은 ACID중 ACI를 만족함.
* Atomical
* Consistently
* In isolation
* Durability

### alter && commute
* ref에 alter를 적용시켰을시 transaction상의 값이 결국 committed 값이됨. 반면 commute는 보장하지 않음.
* commute는 conflict를 발생시키지 않으므로, retry를 위한 transaction 또한 유발하지 않음.

```clojure
(defmacro futures
  [n & exprs]
  (vec (for [_ (range n)
             expr exprs]
         `(future ~expr))))

(defmacro wait-futures
  [& args]
  `(doseq [f# (futures ~@args)]
     @f#))

(def x (ref 0))

(time (wait-futures 5
                    (dotimes [_ 1000]
                      (dosync (alter x + (apply + (range 1000)))))
                    (dotimes [_ 1000]
                      (dosync (alter x - (apply + (range 1000)))))))
;>> "Elapsed time: 1767.158342 msecs"
;=> nil

(time (wait-futures 5
                    (dotimes [_ 1000]
                      (dosync (commute x + (apply + (range 1000)))))
                    (dotimes [_ 1000]
                      (dosync (commute x - (apply + (range 1000)))))))
;>> "Elapsed time: 488.5757 msecs"
;=> nil

```

### ref-set
* 보통, ref의 초기 상태 값을 다시 초기화 시킬때 쓰임.

```clojure
(def x (ref 0))


(deref x)                               ;=> 0
(dosync (ref-set x 10))                 ;=> 10
(dosync (alter x (constantly 20)))      ;=> 20
```

## agents (uncoordinated, asynchronous)

1. 이 agent를 잘 이용하면, I/O나 다른 side-effect를 유발할 수 있는 함수들을 안전하게 처리할 수 있다.
2. retrying하는 transaction에 안전하다.

* send로 queue된 action들은, 현재 하드웨어의 병렬능력을 초과하지 않도록 설정된 고정된 크기의 쓰래드 풀에서 평가된다.
* send-off로 queue된 action들은 제한이 없는 쓰래드풀에서 평가가 된다.


```clojure
(def a (agent 3)) ;=> #'user/a
(deref a)         ;=> 3
(send a range 10) ;=> #<Agent@5c69d74: 3>
(deref a)         ;=> (3 4 5 6 7 8 9)

(def a (agent 5000))                    ;=> #'user/a
(def b (agent 10000))                   ;=> #'user/b
(send-off a #(Thread/sleep %))          ;=> #<Agent@1e4f16a8: 5000>
(send-off b #(Thread/sleep %))          ;=> #<Agent@11f741a: 10000>
(await a b)                             ;=> nil
(deref a)                               ;=> nil

(def a (agent nil))     ;=> #'user/a
(send a (fn [_]
          (throw (Exception. "something is wrong"))))
;=> #<Agent@1d0bc9e9 FAILED: nil>

(restart-agent a 42)    ;=> 42
(send a inc)            ;=> #<Agent@1d0bc9e9: 43>
(reduce send a (for [x (range 3)]
                 (fn [_] (throw (Exception. (str "error #" x))))))
;=> #<Agent@1d0bc9e9: 43>
(agent-error a)         ;=> #<Exception java.lang.Exception: error #0>

(restart-agent a 42)    ;=> 42
(agent-error a)         ;=> #<Exception java.lang.Exception: error #1>

(restart-agent a 42 :clear-actions true) ;=> 42
(agent-error a)                          ;=> nil
```

;; Agent error handler and modes

```
(def a (agent nil :error-mode :continue)) ;=> #'user/a

(send a (fn [_] (throw (Exception. "something is wrong"))))
;=> #<Agent@2c4a560d: nil>

(send a identity)                         ;=> #<Agent@2c4a560d: nil>



(def a (agent nil
              :error-mode :continue
              :error-handler (fn [the-agent exception]
                               (println (.getMessage exception)))))
;=> #'user/a

(send a (fn [_] (throw (Exception. "something is wrong"))))
;=> #<Agent@cf26f68: nil>
;>> something is wrong

(send a identity)                       ;=> #<Agent@cf26f68: nil>

(set-error-handler! a (fn [the-agent exception]
                        (when (= "FATAL" (.getMessage exception))
                          (set-error-mode! the-agent :fail))))
;=> nil


(send a (fn [_] (throw (Exception. "FATAL")))) ;=> #<Agent@cf26f68: nil>
(send a identity)                              ;-> Exception FATAL
```

# Vars
* reference 타입과는 달리, 상태변화가 시간에 대하여 관리되지 않음.

## private
1. 다른 namespace에서 이용하고자 한다면, 완전한 이름을 써야함.
2. var를 손수 deferencing해야 값에 접근할 수 있다.

```clojure
(def ^:private everything 42)
;=> #'user/everything

(ns other-namespace)
;=> nil
(refer 'user)
;=> nil

everyting
;-> CompilerException java.lang.RuntimeException: Unable to resolve symbol

(deref (var user/everything))
;=> 42
```

# Watch
```clojure
;; Watch.
(def sarah (atom {:name "Sarah" :age 25}))
;=> #'user/sarah

(defn echo-watch
  [key identity old new]
  (println key old "=>" new))
;=> #'user/echo-watch


(add-watch sarah :echo echo-watch)
;=> #<Atom@65dd340f: {:age 25, :name "Sarah"}>

(swap! sarah update-in [:age] inc)
;>> :echo {:age 25, :name Sarah} => {:age 26, :name Sarah}
;>> 
;=> {:age 26, :name "Sarah"}

(add-watch sarah :echo2 echo-watch)
;=> #<Atom@65dd340f: {:age 26, :name "Sarah"}>

(swap! sarah update-in [:age] inc)
;>> :echo {:age 26, :name Sarah} => {:age 27, :name Sarah}
;>> 
;>> :echo2 {:age 26, :name Sarah} => {:age 27, :name Sarah}
;>> 
;=> {:age 27, :name "Sarah"}
```

# Validator
```clojure
;; Validators
(def n (atom 1 :validator pos?))
;=> #'user/n

(swap! n + 500)
;=> 501
(swap! n - 1000)
;-> IllegalStateException Invalid reference state

(def sarah (atom {:name "Sarah" :age 25}))
;=> #'user/sarah
(set-validator! sarah :age)
;=> nil
(swap! sarah dissoc :age)
;-> IllegalStateException Invalid reference state

(set-validator! sarah
                #(or (:age %)
                     (throw (IllegalStateException. "People must have `:age`s!"))))
;=> nil

(swap! sarah dissoc :age)
;-> IllegalStateException People must have `:age`s!
```

# Clojure의 STM에서 주의해야될 점.
* retry에 안전한 일을 해야함. I/O처럼 retry시도를 발견하기 힘든경우, 여러번에 걸처 수행될 우려가 있음.
 - io!매크로는 transaction상에서 수행되면 IllegalStateException을 발생함.
* ref로 감싸는 값이 immutable이여야 안전함.
 - mutable일 경우, retry시 원하는 결과를 얻기 힘들지도 모름.


# Locking
- 자바 배열 처럼, 클로저가 제공하는게 아닌 것에 락을 걸어야 할 경우, 몇몇 경우에 한해 locking 매크로가 도움이 될꺼임.
