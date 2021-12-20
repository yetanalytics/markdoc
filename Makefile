.phony: test

test:
	clojure -X:test :dirs '["src/test"]'
