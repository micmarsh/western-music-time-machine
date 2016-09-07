(ns western-music.lib.ui-test
  (:require [clojure.test :refer [deftest is]]
            [western-music.lib.ui :as ui]))

(deftest testing-tests
  (is (= ["ballz"] (ui/new-composition nil "ballz"))))
