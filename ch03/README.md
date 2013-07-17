ch03. Collections and Data Structures
================

## Collection

```clojure
;; Collection
(def l '(1 2 3))                        ; list
(def v [10 20 30])                      ; vector
(def m {:a 100 :b 200})                 ; map
(def s #{1000 2000 3000})               ; set
```

## Sequence

```clojure
;; Sequence
(seq "hello")                           ;=> (\h \e \l \l \o)
(seq [1 2 3])                           ;=> (1 2 3)
(seq {:a 1 :b 2})                       ;=> ([:a 1] [:b 2])
(seq [])                                ;=> nil
(seq nil)                               ;=> nil
```

## rest & next
```clojure
(rest [1])                              ;=> ()
(next [1])                              ;=> nil

(rest nil)                              ;=> ()
(next nil)                              ;=> nil
```

## sequence != list

```clojure
(let [s (range 1e6)]
  (time (count s)))
;>> "Elapsed time: 116.167298 msecs"
;=> 1000000

(let [s (apply list (range 1e6))]
  (time (count s)))
;>> "Elapsed time: 0.016679 msecs"
;=> 1000000
```

## head retention
```clojure
(let [[f s] (split-with #(< % 12) (range 1e8))]
  [(count f) (count s)])
;=> [12 99999988]

(let [[f s] (split-with #(< % 12) (range 1e8))]
  [(count s) (count f)])
;; OutOfMemory
```

## Beware of the nil
```clojure
(remove #{5 7} (cons false (range 10)))
;=> (false 0 1 2 3 4 6 8 9)

(remove #{false 5 7} (cons false (range 10)))
;=> (false 0 1 2 3 4 6 8 9)

(remove (partial contains? #{5 7 false})
        (cons false (range 10)))
;=> (0 1 2 3 4 6 8 9)
```

## transient & persistent!
```clojure
(def x (transient []))
(def y (conj! x 1))

(= x y) ;=> true

(persistent! x) ;=> [1]
(persistent! x) ;-> java.lang.IllegalAccessError: Transient used after persistent! call
```

## into
```clojure
(defn naive-into
  [coll source]
  (reduce conj coll source))

(defn faster-into
  [coll source]
  (persistent! (reduce conj! (transient coll) source)))

(time (do (into #{} (range 1e6))
          nil))
;>> "Elapsed time: 227.952806 msecs"
(time (do (naive-into #{} (range 1e6))
          nil))
;>> "Elapsed time: 437.37291 msecs"
(time (do (faster-into #{} (range 1e6))
          nil))
;>> "Elapsed time: 276.599278 msecs"
```

## MetaData
```clojure
(meta ^:private [1 2 3])           ;=> {:private true}
(meta ^:private ^:dynamic [1 2 3]) ;=> {:dynamic true, :private true}

(def a
  ^{:created (System/currentTimeMillis)}
  [1 2 3])

(meta a)
;=> {:created 1347290765294}

(def b (with-meta a (assoc (meta a)
                      :modified (System/currentTimeMillis))))
(meta b)
;=> {:modified 1347761532079, :created 1347761526881}

(def c (vary-meta a assoc :modified (System/currentTimeMillis)))
(meta c)
;=> {:modified 1347761612033, :created 1347761526881}
```

# 참고자료
* Stuart Halloway - Concurrent Programming with Clojure : http://vimeo.com/8672404
* Persistent Trees in git, Clojure and CouchDB : http://eclipsesource.com/blogs/2009/12/13/persistent-trees-in-git-clojure-and-couchdb-data-structure-convergence/
