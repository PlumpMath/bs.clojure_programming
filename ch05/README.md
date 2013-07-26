Macros
======

# basic usage
```clojure
(def foo 123)                           ;=> #'user/foo
[foo (quote foo) 'foo `foo]             ;=> [123 foo foo user/foo]

(in-ns 'bar)                            ;=> #<Namespace bar>
`foo                                    ;=> bar/foo


(def a 10)         ;=> #'user/a
`(~a)              ;=> (10)

(def b [1 2 3])    ;=> #'user/b
`(~@b)             ;=> (1 2 3)


(defmacro hello [& args] `(+ ~@args))
;=> #'user/hello

(macroexpand-1 '(hello 1 2 3))
;=> (clojure.core/+ 1 2 3)
```

# Hygiene
* Hygienic macro : http://en.wikipedia.org/wiki/Hygienic_macro
 - macro의 expansion이 의도치 못하게 식별자(identifier)들을 capture하지 않는다는 것을 보장하는 매크로.

```clojure
(defmacro hygienic [& body]
  `(let [x :macro-value]
     ~@body))
;=> #'user/hygienic

(let [x 10]
  (hygienic
   (println x)))
;-> CompilerException java.lang.RuntimeException: Can't let qualified name: user/x


(defmacro hygienic [& body]
  (let [x (gensym "x")]
    `(let [~x :macro-value]
       ~@body)))
;=> #'user/hygienic

(let [x 10]
  (hygienic
   (println x)))
;>> 10
;=> nil


(defmacro hygienic [& body]
  `(let [x# :macro-value]
     ~@body))
;=> #'user/hygienic

(let [x 10]
  (hygienic
   (println x)))
;>> 10
;=> nil
```


# &env
```clojure
(defmacro spy-env []
  (let [ks (keys &env)]
    `(println (zipmap '~ks [~@ks]))))
;=> #'user/spy-env

(let [x 10 y 20]
  (spy-env))
;>> {x 10, y 20}
;=> nil
```

# &form
```clojure
(defmacro test-form [& test]
  (let [form &form]
  `(println '~form)))
;=> #'user/test-form

(test-form 1 2 3)
;>> (test-form 1 2 3)
;=> nil
```
