Concurrency and Parallelism
==========

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

```

## refs (coordinated, synchronous)
```clojure

```

## agents (uncoordinated, asynchronous)
```clojure

```



# Vars
* reference 타입과는 달리, 상태변화가 시간에 대하여 관리되지 않음.
