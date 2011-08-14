;; Copyright (C) 2011
;;
;; Distributed under the Eclipse Public License, the same as Clojure.
;;
;; Author: Ole Rixmann <rixmann.ole@googlemail.com>

(ns clojure.otp.util)

(defn module_resolve [Module Function]
;  (try
    (ns-resolve (symbol Module) (symbol Function))
);    (catch java.lang.Exception e false)))
