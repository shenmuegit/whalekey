"""The real t2s conversion must merge pairs by max weight and order ties."""
import os
import tempfile
import unittest
from pathlib import Path
from prepare_predict import prepare


class PreparePredictTest(unittest.TestCase):
    def test_simplifies_both_fields_merges_maximum_and_orders_ties(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).parent.parent / ".gradle") as directory:
            source, output = Path(directory) / "input.txt", Path(directory) / "output.txt"
            source.write_text("中國\t經濟\t10\n中国\t经济\t20\n中国\t经济\t5\n中國\t人民\t20\n謝謝\t你們\t7\n中國\t的\t1\n", encoding="utf-8")
            prepare(source, output, os.environ["OPENCC_T2S_CONFIG"])
            self.assertEqual(output.read_text(encoding="utf-8"),
                             "中国\t人民\t20\n中国\t经济\t20\n中国\t的\t1\n谢谢\t你们\t7\n")


if __name__ == "__main__":
    unittest.main()
