(ns user
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [integrant.repl :as ir]
            [integrant.repl.state :as irs]))

(defn- prep
  []
  (-> (io/resource "daveduthie/load-shedding-calendar/config.edn")
      slurp
      edn/read-string
      :system
      ig/prep))

(ir/set-prep! prep)

(defn stop [] (ir/halt))

(defn go [] (ir/go))

(defn system [] irs/system)

(comment
  (ir/reset))
