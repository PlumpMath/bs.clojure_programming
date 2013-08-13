Datatypes and Protocols
=======================


# Protocls


extend-type
extend-protocol


# Types
자바 클래스로 변환되기에, lower-dashed-case로 쓰지 않고, CamelCase로 쓴다.

# defrecord
* Value semantics
 - record는 immutable이다.
 - 두 record의 필드가 같으면, 두 record는 동일한 것.
* Full participation in the associative collection abstraction
* Metadata support
* Reader support, so instances of record types can be created by simply reading data
* An additional convenience constructor for creating records with metadata and auxiliary fields as desired

```clojure
(defrecord ARecord [x y])               ;=> user.ARecord

(ARecord. 1 2)                          ;=> #user.ARecord{:x 1, :y 2}
(= (ARecord. 1 2) (ARecord. 1 2))       ;=> true

(.x (ARecord. 1 2))                     ;=> 1
(:x (ARecord. 1 2))                     ;=> 1
({:x 1 :y 2} :x)                        ;=> 1
(:x {:x 1 :y 2})                        ;=> 1

((ARecord. 1 2) :x) ;-> ClassCastException user.ARecord cannot be cast to clojure.lang.IFn
(:z (ARecord. 1 2) -99)                 ;=> -99

(assoc (ARecord. 1 2) :z 3)        ;=> #user.ARecord{:x 1, :y 2, :z 3}
(:z (assoc (ARecord. 1 2) :z 3))   ;=> 3
(.z (assoc (ARecord. 1 2) :z 3)) ;-> IllegalArgumentException No matching field found: z for class user.ARecord


(->ARecord 1 2)                    ;=> #user.ARecord{:x 1, :y 2}
(map->ARecord {:x 1, :y 2, :z 3})  ;=> #user.ARecord{:x 1, :y 2, :z 3}


(ARecord/getBasis)                      ;=> [x y]
```

# deftype

