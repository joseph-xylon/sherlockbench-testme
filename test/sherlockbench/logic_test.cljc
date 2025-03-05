(ns sherlockbench.logic-test
  (:require [sherlockbench.logic :as logic]
            [clojure.test :refer [deftest is testing run-tests]]))

(deftest add-function-names-test
  (testing "add-function-names combines data from attempts and problem-names"
    (let [attempts [{:attempt-id "b0026371-58d9-4d81-beec-be975cf5d0ca",
                     :arg-spec ["integer" "integer" "integer"],
                     :problem-name "Problem 1",
                     :state :completed,
                     :result {{"wrong" "wrong", "done" "correct"} "done"}}
                    {:attempt-id "94df8356-3897-4e1f-ae32-1967f0507a59",
                     :arg-spec ["integer"],
                     :problem-name "Problem 2",
                     :state :abandoned}
                    {:attempt-id "6cd92a5a-e37b-4400-8eb6-b65bd3cb8403",
                     :arg-spec ["integer"], 
                     :problem-name "Problem 3", 
                     :state :abandoned}]
          
          problem-names [{:id "b0026371-58d9-4d81-beec-be975cf5d0ca",
                           :function_name "add & subtract"}
                          {:id "94df8356-3897-4e1f-ae32-1967f0507a59",
                           :function_name "is prime"}
                          {:id "6cd92a5a-e37b-4400-8eb6-b65bd3cb8403",
                           :function_name "modulus 3 to fruit"}]
          
          expected [{:attempt-id "b0026371-58d9-4d81-beec-be975cf5d0ca",
                     :arg-spec ["integer" "integer" "integer"],
                     :problem-name "Problem 1",
                     :state :completed,
                     :result {{"wrong" "wrong", "done" "correct"} "done"},
                     :function_name "add & subtract"}
                    {:attempt-id "94df8356-3897-4e1f-ae32-1967f0507a59",
                     :arg-spec ["integer"],
                     :problem-name "Problem 2",
                     :state :abandoned,
                     :function_name "is prime"}
                    {:attempt-id "6cd92a5a-e37b-4400-8eb6-b65bd3cb8403",
                     :arg-spec ["integer"], 
                     :problem-name "Problem 3", 
                     :state :abandoned,
                     :function_name "modulus 3 to fruit"}]
          
          result (logic/add-function-names attempts problem-names)]
      
      (is (= expected result)
          "Should add function_name to each attempt based on matching id")))
  
  (testing "add-function-names with mismatched IDs"
    (let [attempts [{:attempt-id "b0026371-58d9-4d81-beec-be975cf5d0ca",
                     :problem-name "Problem 1"}
                    {:attempt-id "not-in-problem-names",
                     :problem-name "Problem 2"}]
          
          problem-names [{:id "b0026371-58d9-4d81-beec-be975cf5d0ca",
                           :function_name "add & subtract"}
                          {:id "some-other-id",
                           :function_name "is prime"}]
          
          expected [{:attempt-id "b0026371-58d9-4d81-beec-be975cf5d0ca",
                     :problem-name "Problem 1",
                     :function_name "add & subtract"}
                    {:attempt-id "not-in-problem-names",
                     :problem-name "Problem 2"}]
          
          result (logic/add-function-names attempts problem-names)]
      
      (is (= expected result)
          "Should only add function_name when attempt-id matches id in problem-names")))
  
  (testing "add-function-names with empty collections"
    (is (= [] (logic/add-function-names [] []))
        "Should handle empty collections")
    
    (is (= [{:attempt-id "test-id"}] 
           (logic/add-function-names [{:attempt-id "test-id"}] []))
        "Should return attempts unchanged when problem-names is empty")
    
    (is (= [] 
           (logic/add-function-names [] [{:id "test-id", :function_name "test"}]))
        "Should return empty list when attempts is empty")))

(comment
  (run-tests)
)