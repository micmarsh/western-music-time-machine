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

(def with-composers-set
  (gen/fmap (fn [{compositions :data/raw :as all-data}]
              (let [composers (distinct (map comp/composer-name compositions))]
                {:composers composers
                 :all all-data}))
            all-data-gen))

;; (defspec test-select-composer-data-spec 30
;;   (prop/for-all
;;    [sample with-composers-set]
;;    (try
;;      (spec/verify
;;       ui/all-data-spec
;;       (ui/select-composer (:all sample) (first (:composers sample))))
;;      (catch clojure.lang.ExceptionInfo e
;;        (println (ex-data e))
;;        false))))

;; (defspec test-select-composer-ui-tracks 30
;;   (prop/for-all
;;    [sample with-composers-set]
;;    (let [composer-name (first (:composers sample))
;;          with-selected (ui/select-composer (:all sample) composer-name)]
;;      (->> (:data/ui with-selected)
;;           (ui/track-list)
;;           (map :track/artist)
;;           (every? #{composer-name})))))

(defspec fancy-rng-trick {:num-tests 10 :seed 1473539331217}
  (prop/for-all [i (s/gen int?)]                  
                (binding [western-music.util/*random-seed* (Math/abs (hash i))]
                  (println {:prop-int i :fancy-choice (western-music.util/random (seq [:one :two :three :four :five :six :seven]))})) true))
