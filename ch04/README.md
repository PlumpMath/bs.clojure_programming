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
