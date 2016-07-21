(ns western-music.ingest.wikidata)

(def ^:const properties
  {:place-of-birth "P19"
   :date-of-birth "P569"
   :title "P373" ;; technically "Commons Category". Regardless, seems to be a
   ;; good way to cut to the chase and get an identifier
   
   :first-name "P735"
   :last-name "P1477"})

;; "mainsnak" seems to be an important part of this shit, where the
;; main value is found


(def ^:const)
(def ^:const)
(def ^:const)
