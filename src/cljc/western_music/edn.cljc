(ns western-music.edn)

(defn key-tag-parser [key]
  (fn [m]
    (into { }
          (for [[k v] m]
            [(if (namespace k)
               k
               (keyword (name key) (name k)))
             v]))))

(def ^:const initial-data-keys
  [:track :composer :composition:western-music.spec])

(defn register-custom-readers! []
  (doseq [[key parser] (map (juxt identity key-tag-parser) initial-data-keys)]
    #?(:cljs (cljs.reader/register-tag-parser! key parser))))
