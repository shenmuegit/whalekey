"""Prepare the bundled Rime data with official OpenCC 1.2.0 t2s."""
import argparse
import json
from pathlib import Path
from opencc import OpenCC


def prepare(source, output, config):
    converter = OpenCC(str(config))
    relations = {}
    input_count = 0
    for line in Path(source).read_text(encoding="utf-8").splitlines():
        context, continuation, weight = line.split("\t")
        pair = converter.convert(context), converter.convert(continuation)
        weight = int(weight)
        relations[pair] = max(relations.get(pair, weight), weight)
        input_count += 1
    ordered = sorted(relations.items(), key=lambda item: (item[0][0], -item[1], item[0][1]))
    with Path(output).open("w", encoding="utf-8", newline="\n") as result:
        for (context, continuation), weight in ordered:
            result.write(f"{context}\t{continuation}\t{weight}\n")
    return {"input_relations": input_count, "output_relations": len(relations),
            "output_contexts": len({context for context, _ in relations}),
            "collapsed_relations": input_count - len(relations)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("config", type=Path, help="Pinned OpenCC t2s.json and its compiled dictionaries")
    args = parser.parse_args()
    print(json.dumps(prepare(args.source, args.output, args.config), sort_keys=True))
