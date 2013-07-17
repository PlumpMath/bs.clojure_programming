(def *out*-logger (print-logger *out*))

(*out*-logger "hello")

;; ---
(def writer (java.io.StringWriter.))
(def retained-logger (print-logger writer))
(retained-logger "hello")

(str writer) ;=> "hello\n"

;; ---
(require 'clojure.java.io)

(defn file-logger [file]
  (fn [msg]
    (with-open [f (clojure.java.io/writer file :append true)]
      ((print-logger f) msg))))

(def log->file (file-logger "messages.log"))

(log->file "hello")

(slurp "messages.log") ;=> "hello\n"

;; ---
(defn multi-logger [& logger-fns]
  (fn [msg]
    (doseq [f logger-fns] (f msg))))


(def log (multi-logger (print-logger *out*)
                       (file-logger "messages.log")))
(log "hello again")
(slurp "messages.log") ;=> "hello\nhello again\n"

;; ---
(defn timestamped-logger
  [logger]
  (fn [msg] (logger (format "%1$tY-%1$tm-%1$te %1$tH:%1$tM:%1$tS"
                            (java.util.Date.) msg))))

(def log-timestamped
  (timestamped-logger (multi-logger
                       (print-logger *out*)
                       (file-logger "messages.log"))))

(log-timestamped "goodbye, now")
(slurp "messages.log") ;=> "hello\nhello again\n2013-07-16 08:41:28\n"
