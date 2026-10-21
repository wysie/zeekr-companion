from pathlib import Path
import os
import tempfile
import unittest
from release_notes import changelog_section, translated_sections


class ReleaseNotesTranslationTest(unittest.TestCase):
    def test_current_notes_have_both_authored_translations(self):
        root = Path(__file__).resolve().parents[2]
        old = Path.cwd()
        try:
            os.chdir(root)
            for version in ['Unreleased'] + ['2.11.' + str(n) + '-alpha.1' for n in range(56, 61)]:
                with self.subTest(version=version):
                    english = changelog_section(version)
                    blocks = translated_sections(version)
                    self.assertIn('<!-- companion-notes:ms -->', blocks)
                    self.assertIn('<!-- companion-notes:zh-CN -->', blocks)
                    # One translated bullet for each English change; don't silently omit fixes.
                    count = sum(line.startswith('- ') for line in english.splitlines())
                    for locale in ['ms', 'zh-CN']:
                        section = changelog_section(version, 'CHANGELOG.' + locale + '.md')
                        self.assertEqual(count, sum(line.startswith('- ') for line in section.splitlines()))
        finally:
            os.chdir(old)

    def test_new_release_requires_both_translations(self):
        old = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                Path('CHANGELOG.ms.md').write_text('## [1.0.0]\n\n- Bahasa Melayu\n', encoding='utf-8')
                with self.assertRaisesRegex(ValueError, 'zh-CN'):
                    translated_sections('1.0.0')
            finally:
                os.chdir(old)
