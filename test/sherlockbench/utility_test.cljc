(ns sherlockbench.utility-test
  (:require [sherlockbench.utility :as utility]
            [clojure.test :refer [deftest is testing run-tests]]))

(deftest valid-uuid-test
   (testing "Valid UUIDs"
     (is (utility/valid-uuid? "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
         "Should return true for a valid lowercase UUID")
     (is (utility/valid-uuid? "F81D4FAE-7DEC-11D0-A765-00A0C91E6BF6")
         "Should return true for a valid uppercase UUID")
     (is (utility/valid-uuid? "00000000-0000-0000-0000-000000000000")
         "Should return true for all zeros UUID")
     (is (utility/valid-uuid? "ffffffff-ffff-ffff-ffff-ffffffffffff")
         "Should return true for all f's UUID"))
 
   (testing "Invalid UUIDs"
     (is (not (utility/valid-uuid? "invalid-uuid-format"))
         "Should return false for an invalid format")
     (is (not (utility/valid-uuid? "f81d4fae7dec11d0a76500a0c91e6bf6"))
         "Should return false for missing hyphens")
     (is (not (utility/valid-uuid? "f81d4fae-7dec-11d0-a765-00a0c91e6bf"))
         "Should return false for too short UUID")
     (is (not (utility/valid-uuid? "f81d4fae-7dec-11d0-a765-00a0c91e6bff6"))
         "Should return false for too long UUID")
     (is (not (utility/valid-uuid? 12345))
         "Should return false for a number")
     (is (not (utility/valid-uuid? nil))
         "Should return false for nil")
     (is (not (utility/valid-uuid? ""))
         "Should return false for an empty string")
     (is (not (utility/valid-uuid? "f81d4fae-7dec-11d0-a765-00a0c91e6bg6"))
         "Should return false for invalid hex characters")))

(comment
  (run-tests)
  )
