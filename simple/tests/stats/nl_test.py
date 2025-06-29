# Copyright 2023 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import shutil
import tempfile
import unittest

from stats.data import Triple
import stats.nl as nl
import stats.schema_constants as sc
from tests.stats.test_util import compare_files
from tests.stats.test_util import is_write_mode
from tests.stats.test_util import read_triples_csv
from util.filesystem import create_store

_TEST_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                              "test_data", "nl")
_INPUT_DIR = os.path.join(_TEST_DATA_DIR, "input")
_EXPECTED_DIR = os.path.join(_TEST_DATA_DIR, "expected")

# Temp dir paths in test catalog files will be rewritten with this fake path
# so they can be compared with golden files with a constant fake path.
_FAKE_PATH = "//fake/path"


def _rewrite_catalog_for_testing(catalog_yaml_path: str, temp_dir: str) -> None:
  """
  Test catalog files are written to temp folders whose values change from test to test.
  To consistently test the catalog out against a golden file, we replace the temp paths
  with a constant fake path.
  """
  with create_store(catalog_yaml_path) as store:
    catalog_file = store.as_file()
    content = catalog_file.read()
    content = content.replace(temp_dir, _FAKE_PATH)
    catalog_file.write(content)


def _test_generate_nl_sentences(test: unittest.TestCase,
                                test_name: str,
                                generate_topic_cache: bool = False):
  test.maxDiff = None

  with tempfile.TemporaryDirectory() as temp_dir:
    temp_store = create_store(temp_dir)
    input_triples_path = os.path.join(_INPUT_DIR, f"{test_name}.csv")
    input_triples = read_triples_csv(input_triples_path)

    output_sentences_csv_path = os.path.join(temp_dir, "sentences.csv")
    expected_sentences_csv_path = os.path.join(_EXPECTED_DIR, test_name,
                                               "sentences.csv")

    output_catalog_yaml_path = os.path.join(temp_dir, "embeddings",
                                            "custom_catalog.yaml")
    expected_catalog_yaml_path = os.path.join(_EXPECTED_DIR, test_name,
                                              "custom_catalog.yaml")

    output_topic_cache_json_path = os.path.join(temp_dir,
                                                "custom_dc_topic_cache.json")
    expected_topic_cache_json_path = os.path.join(_EXPECTED_DIR, test_name,
                                                  "custom_dc_topic_cache.json")

    # Sentences are not generated for StatVarPeerGroup triples.
    # So remove them first.
    nl.generate_nl_sentences(_without_svpg_triples(input_triples),
                             nl_dir=temp_store.as_dir())
    _rewrite_catalog_for_testing(output_catalog_yaml_path, temp_dir)

    if generate_topic_cache:
      # Topic cache is not generated for SV triples.
      # So remove them first.
      nl.generate_topic_cache(_without_sv_triples(input_triples),
                              nl_dir=temp_store.as_dir())

    if is_write_mode():
      shutil.copy(output_sentences_csv_path, expected_sentences_csv_path)
      shutil.copy(output_catalog_yaml_path, expected_catalog_yaml_path)
      if generate_topic_cache:
        shutil.copy(output_topic_cache_json_path,
                    expected_topic_cache_json_path)
      return

    compare_files(test, output_sentences_csv_path, expected_sentences_csv_path)
    compare_files(test, output_catalog_yaml_path, expected_catalog_yaml_path)
    if generate_topic_cache:
      compare_files(test, output_topic_cache_json_path,
                    expected_topic_cache_json_path)

    temp_store.close()


def _without_svpg_triples(triples: list[Triple]) -> list[Triple]:
  svpg_dcids = set()
  for triple in triples:
    if triple.predicate == sc.PREDICATE_TYPE_OF and triple.object_id == sc.TYPE_STAT_VAR_PEER_GROUP:
      svpg_dcids.add(triple.subject_id)

  if not svpg_dcids:
    return triples
  return list(
      filter(lambda triple: triple.subject_id not in svpg_dcids, triples))


def _without_sv_triples(triples: list[Triple]) -> list[Triple]:
  sv_dcids = set()
  for triple in triples:
    if triple.predicate == sc.PREDICATE_TYPE_OF and triple.object_id == sc.TYPE_STATISTICAL_VARIABLE:
      sv_dcids.add(triple.subject_id)

  if not sv_dcids:
    return triples
  return list(filter(lambda triple: triple.subject_id not in sv_dcids, triples))


class TestData(unittest.TestCase):

  def test_generate_nl_sentences_for_sv_triples(self):
    _test_generate_nl_sentences(self, "sv_triples")

  def test_generate_nl_sentences_for_topic_triples(self):
    _test_generate_nl_sentences(self,
                                "topic_triples",
                                generate_topic_cache=True)

  def test_generate_nl_sentences_for_sv_and_topic_triples(self):
    _test_generate_nl_sentences(self,
                                "sv_and_topic_triples",
                                generate_topic_cache=True)
