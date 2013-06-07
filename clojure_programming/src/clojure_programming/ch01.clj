(ns ch01)

;;; ch01. Down the Rabbit Hole
;; 토끼굴로 내려가다

;;;; 왜 클로져인가?
;; 1. JVM 상에서 돌아간다.
;; 2. Lisp이다.
;; 3. 함수형 프로그래밍 언어이다.
;; 4. concurrency와 parallelization에서의 문제를 해결하는데 있어, 획기적인 해결책을 제시한다
;; 5. dynamically하며 strongly typed 언어이다.

;;;; Homoiconicity
;; 코드가 데이터이고, 데이터가 코드이다.
;; AST로 변환되는 문법을 정의하는대신, AST를 표현하는 자료구조를 가지고 프로그램을 작성한다.

;;;; 여러가지 Reader Sugar
(quote a)                               ; a
'a                                      ; a
(= (quote a) 'a)                        ; true

((fn [x] (+ x 2)) 1)                    ; 3
(#(+ % 2) 1)                            ; 3
(= (fn [x] (+ x 2)) #(+ % 2))           ; false

;;;; Destructuring
(def v [1 2 3])
(let [[x _ y :as z] v]
  (prn x)                               ; 1
  (prn y)                               ; 3
  (prn z))                              ; [1 2 3]

;; Map destructuring
(def m {:a 5
        :b 6
        :c [7 8 9]
        :d {:e 10
            :f 11}
        "foo" 88
        42 false})

(let [{x :a                             ; x => :a 5
       y :??                            ; y => :?? ?
       :or {y 50}} m]                   ; y => 50
  (+ x y))                              ;55


(let [{x :a y :??} m                    ; x => :a 5, y => :?? ?
      y (or y 50)]                      ; y => 50
  (+ x y))                              ;55


;;;; Looping: loop and recur
;; recur이 필요없을 경우.
;; 1. doseq와 dotimes와 같은 상위 레벨 looping을 이용하여 처리할 수 있을 경우.
;; 2. map, reduce, for와 같이 collection이나 sequence를 처리할 때.


(class #"(\d+)-(\d+)")
(re-seq #"(\d+)-(\d+)" "1-3")
(= [1 2 3] [1, 2, 3])
(= {:a 1 :b 2} {:a 1, :b 2})
