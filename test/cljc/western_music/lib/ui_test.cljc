(ns western-music.lib.ui-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test :refer [run-tests]]
            [western-music.spec :as spec]
            [western-music.lib.ui :as ui]
            [western-music.lib.composition :as comp]
            [clojure.test.check :as tc]
            [clojure.spec.gen :as gen]
            [clojure.spec :as s]))

(def all-data-gen (s/gen ui/all-data-spec))

(def with-composer-set
  (gen/fmap (fn [{compositions :data/raw :as all-data}]
              (let [composers (distinct (map comp/composer-name compositions))]
                {:composers composers
                 :all all-data}))
            all-data-gen))

(defspec test-select-composer-data-spec 30
  (prop/for-all
   [sample with-composer-set]
   (try
     (spec/verify
      ui/all-data-spec
      (ui/select-composer (:all sample) (first (:composers sample))))
     (catch clojure.lang.ExceptionInfo e
       (println (ex-data e))
       false))))

(defspec test-select-composer-ui-tracks 30
  (prop/for-all
   [sample with-composer-set]
   (let [composer-name (first (:composers sample))
         with-selected (ui/select-composer (:all sample) composer-name)]
     (->> (:data/ui with-selected)
          (ui/track-list)
          (map comp/composer-name)
          (every? #{composer-name})))))
