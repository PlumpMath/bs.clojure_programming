ch02. Functional Programming
==========================================

## What Does Functional Programming Mean?

각각 언어마다 다르게 정의하지만, Clojure에서의 함수형프로그래밍이란?

* immutable 값을 다루는 것을 선호.
 - mutable한 상태말고, 단순한 추상계층(abstractions)를 만족하는 immutable 자료구조를 사용.
 - higher-order 함수로써(take one or more functions as an input, output a function), 함수를 값 자체로 다룸.
* A preference for declarative processing of data over imperative control structures and iteration.
* 복잡한 문제를 풀기 위해, 보다 고차원적으로, 함수 composition, higher-order 함수, immutable 자료구조의 활용이 점진적으로 늘어나는 환경.


```clojure
(#(map * %1 %2 %3) [1 2 3] [4 5 6] [7 8 9]) ;=> (28 80 162)
(#(apply map * %&) [1 2 3] [4 5 6] [7 8 9]) ;=> (28 80 162)
((partial map *)   [1 2 3] [4 5 6] [7 8 9]) ;=> (28 80 162)

((comp str +) 1 2 3 4) ;=> "10"

(-> 3
    (/ 2)) ;=> 3/2

(->> 3
     (/ 2)) ;=> 2/3
```


* Pure Functions

```clojure
(repeatedly 10 (partial rand-int 10))           ;=> (5 6 8 2 7 7 4 5 6 5)
(repeatedly 10 (partial (memoize rand-int) 10)) ;=> (6 6 6 6 6 6 6 6 6 6)
```
