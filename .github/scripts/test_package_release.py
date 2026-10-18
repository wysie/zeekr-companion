import json
from pathlib import Path
import tempfile
import unittest
from package_release import release_identity, stage


class ReleasePackagingTest(unittest.TestCase):
    def test_standard_names_and_channels(self):
        for version, prerelease in [("2.11.42-alpha.1", True), ("2.11.42-beta.2", True), ("2.11.42", False)]:
            identity = release_identity(version)
            self.assertEqual("v" + version, identity["tag"])
            self.assertEqual(f"ZeekrCompanion-{version}.apk", identity["filename"])
            self.assertEqual("Zeekr Companion " + version, identity["title"])
            self.assertEqual(prerelease, identity["prerelease"])

    def test_refuses_ambiguous_names(self):
        for version in ["fork-2.11.42-alpha", "2.11.42-personal-alpha", "2.11.42-beta", "2.11.42-rc.1", "2.11.42-beta.01", "02.11.42", "2.11.42-alpha.0"]:
            with self.subTest(version=version), self.assertRaises(ValueError):
                release_identity(version)

    def test_stages_exact_built_bytes_and_checksum(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "app-release.apk"
            source.write_bytes(b"abc")
            metadata = {"applicationId": "io.github.dts88.zeekrshortcut", "variantName": "release",
                        "elements": [{"versionName": "2.11.42-alpha.1", "versionCode": 325,
                                      "filters": [], "outputFile": source.name}]}
            path = root / "output-metadata.json"
            path.write_text(json.dumps(metadata))
            with self.assertRaises(ValueError):
                stage(path, root / "edition", "v2.11.41-alpha.1")
            self.assertFalse((root / "edition").exists())
            result = stage(path, root / "edition", "v2.11.42-alpha.1")
            self.assertEqual(source.read_bytes(), Path(result["path"]).read_bytes())
            self.assertEqual("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result["sha256"])
            self.assertEqual(f'{result["sha256"]}  {result["filename"]}\n', Path(result["checksum"]).read_text())
            self.assertEqual(result, json.loads((root / "edition/release-manifest.json").read_text()))
            metadata["applicationId"] = "wrong.app"
            path.write_text(json.dumps(metadata))
            with self.assertRaises(ValueError):
                stage(path, root / "wrong")


if __name__ == "__main__":
    unittest.main()
