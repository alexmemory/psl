(ns {{namespace}}
  "Define a PSL model and run inference."
  (:require
   [incanter.core :as in]
   [incanter-util.core :as iu]
   [clojure.java.io :as io]
   [clj-psl.core :as psl]
   )
  (:import
   [edu.umd.cs.psl.model.argument ArgumentType]
   [edu.umd.cs.psl.database.rdbms.driver H2DatabaseDriver H2DatabaseDriver$Type]
   ))

;;; Predicates

(defn VOCAB
  "Add predicates to the model"
  [model]
  (psl/add-predicate model dog
                    {"types" (repeat 1 ArgumentType/UniqueID)
                     "names" ["n"]})
  (psl/add-predicate model mammal
                    {"types" (repeat 1 ArgumentType/UniqueID)
                     "names" ["n"]})
  model)

;;; Rules

(defn DOGS-ARE-MAMMALS
  "Add a rule that all dogs are mammals."
  [model weight squared]
  (psl/add-rule model
               (psl/IMPL
                (dog 'N)
                (mammal 'N))
               weight squared "DOGS-ARE-MAMMALS")
  model)

;;; Handy functions --- modify to suit

(defn config-bundle
  "A config bundle"
  ([]
   (config-bundle "psl"))
  ([bundle-name]
   (let [cm (. edu.umd.cs.psl.config.ConfigManager getManager)]
     (.getBundle cm bundle-name))))

(defn data-store
  "A data store"
  ([config-bundle]
   (data-store config-bundle true "."))
  ([config-bundle clear-db output-dir]
   (let [db-path (.getPath (io/file output-dir "psl"))
         driver (H2DatabaseDriver. H2DatabaseDriver$Type/Disk db-path clear-db)]
     (edu.umd.cs.psl.database.rdbms.RDBMSDataStore. driver config-bundle))))

(defn inference
  "Run inference"
  [cb ds m obs res closed-preds]
  (let [;; A view for reading observations and writing inferences
        db (psl/open-db ds m [obs] res closed-preds)

        ;; An inference application
        inf (psl/default-inference m db cb)

        ;; A reasoner
        rea (psl/default-reasoner cb)]

    ;; Run inference
    (psl/mpe-inference inf rea)

    ;; Retrieve ground kernels
    (def gks (.getGroundKernels rea))

    ;; Clean up
    (psl/close-db db)
    (.close rea)
    (.close inf)))
