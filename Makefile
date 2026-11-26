.PHONY: build clean

debug:
	./gradlew assembleDebug

test:
	./gradlew testDebugUnitTest

release:
	./gradlew bundleRelease

install: build
	./gradlew installDebug

install-release:
	./gradlew installRelease

uninstall:
	./gradlew uninstallAll

clean:
	./gradlew clean

lint:
	./gradlew app:lint

