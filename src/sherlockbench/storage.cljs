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
  "Retrieves attempt data from local storage by attempt-id"
  [attempt-id]
  (get local-storage (str "attempt-" attempt-id)))

(defn set-attempt!
  "Stores attempt data in local storage with attempt-id as key"
  [attempt-id attempt-data]
  (assoc! local-storage (str "attempt-" attempt-id) attempt-data))

(comment
  ;; example of how the contents of the store looks:

  {:run-type "anonymous",
   :attempts
   '({:attempt-id "2cc2ef98-3718-457e-b3db-8c5f93ba4d69",
      :arg-spec ["integer" "integer" "integer"],
      :problem-name "Problem 1",
      :state :investigate}),
   :run-id "233e6dab-227c-48c3-ab71-a99842d638a0",
   :benchmark-version "0.1.0"}

  )
