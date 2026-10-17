import unittest
from validate_curriculum import excess_words


class RepetitionPolicyTests(unittest.TestCase):
    def test_five_standalone_targets_are_allowed(self):
        self.assertEqual(excess_words([{'speech': 'Apple'}] * 5)[0], {})

    def test_sixth_target_is_rejected_across_case_and_punctuation(self):
        questions = [{'speech': w} for w in ['Apple', 'apple', 'APPLE', 'apple.', 'apple!', ' apple? ']]
        self.assertEqual(excess_words(questions)[0], {'apple': 6})

    def test_phrases_do_not_consume_word_budget(self):
        questions = [{'speech': 'Apple'}] * 5 + [{'speech': 'I have an apple.'}] * 50
        self.assertEqual(excess_words(questions)[0], {})

    def test_distractors_are_not_learning_targets(self):
        questions = [{'speech': 'I see two birds.', 'choices': ['Apple', 'Orange']}] * 10
        self.assertEqual(excess_words(questions)[0], {})

    def test_contractions_are_normalized(self):
        questions = [{'speech': "don't"}] * 5 + [{'speech': 'DON’T!'}]
        self.assertEqual(excess_words(questions)[0], {"don't": 6})


if __name__ == '__main__':
    unittest.main()
