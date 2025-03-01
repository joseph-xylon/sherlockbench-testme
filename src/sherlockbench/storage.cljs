(ns sherlockbench.storage
  (:require [hodgepodge.core :refer [local-storage clear!]]))

(defn get-run
  "Retrieves run data from local storage by run-id"
  [run-id]
  (get local-storage (str "run-" run-id)))

(defn set-run!
  "Stores run data in local storage with run-id as key"
  [run-id run-data]
  (assoc! local-storage (str "run-" run-id) run-data))

(defn get-attempt
  "Retrieves attempt log from local storage by attempt-id"
  [attempt-id]
  (get local-storage (str "attempt-" attempt-id)))

(defn set-attempt!
  "Stores attempt log in local storage with attempt-id as key"
  [attempt-id attempt-log]
  (assoc! local-storage (str "attempt-" attempt-id) attempt-log))
