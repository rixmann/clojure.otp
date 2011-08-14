(ns clojure.otp.test.random_server
  (:require [clojure.otp.gen_server :as gen_server])
  (:use pattern-match))

(def module 'clojure.otp.test.random_server)

;;api
(defn start_link []
  (gen_server/start_link module))

(defn produce_a_random [Srv]
  (gen_server/cast Srv :produce))

(defn get_a_random [Srv]
  (gen_server/call Srv :prod_and_answer))

(defn stop [Srv]
  (gen_server/cast Srv :stop))

(defn init []
  (java.util.Random.))

(defn handle_cast [P State]
  (if (= :produce P)
    (do
      (.nextInt State)
      [:noresponse State])
    [:stop :normal State]))

(defn handle_call [Message State From]
  (if (= Message :prod_and_answer)
    [:response (.nextInt State) State]
    [:noresponse State]))
    ;; Val (.. System out (println (with-out-str (print "Error while matching in random_server, got return value from state-fn: " Val)))))

(defn terminate [Reason State]
  :terminated)
