import bisect
import re


class NFA(object):
    EPSILON = object()
    ANY = object()

    def __init__(self, start_state):
        self.transitions = {}
        self.final_states = set()
        self._start_state = start_state

    @property
    def start_state(self):
        return frozenset(self._expand({self._start_state}))

    def add_transition(self, src, input, dest):
        self.transitions.setdefault(src, {}).setdefault(input, set()).add(dest)

    def add_final_state(self, state):
        self.final_states.add(state)

    def is_final(self, states):
        return self.final_states.intersection(states)

    def _expand(self, states):
        frontier = set(states)
        while frontier:
            state = frontier.pop()
            new_states = self.transitions.get(state, {}).get(NFA.EPSILON, set()).difference(states)
            frontier.update(new_states)
            states.update(new_states)
        return states

    def next_state(self, states, input):
        dest_states = set()
        for state in states:
            state_transitions = self.transitions.get(state, {})
            dest_states.update(state_transitions.get(input, []))
            dest_states.update(state_transitions.get(NFA.ANY, []))
        return frozenset(self._expand(dest_states))

    def get_inputs(self, states):
        inputs = set()
        for state in states:
            inputs.update(self.transitions.get(state, {}).keys())
        return inputs

    def to_dfa(self):
        dfa = DFA(self.start_state)
        frontier = [self.start_state]
        seen = set()
        while frontier:
            current = frontier.pop()
            inputs = self.get_inputs(current)
            for input in inputs:
                if input == NFA.EPSILON:
                    continue
                new_state = self.next_state(current, input)
                if new_state not in seen:
                    frontier.append(new_state)
                    seen.add(new_state)
                    if self.is_final(new_state):
                        dfa.add_final_state(new_state)
                if input == NFA.ANY:
                    dfa.set_default_transition(current, new_state)
                else:
                    dfa.add_transition(current, input, new_state)
        return dfa


class DFA(object):
    def __init__(self, start_state):
        self.start_state = start_state
        self.transitions = {}
        self.defaults = {}
        self.final_states = set()

    def add_transition(self, src, inpt, dest):
        self.transitions.setdefault(src, {})[inpt] = dest

    def set_default_transition(self, src, dest):
        self.defaults[src] = dest

    def add_final_state(self, state):
        self.final_states.add(state)

    def is_final(self, state):
        return state in self.final_states

    def next_state(self, src, input):
        state_transitions = self.transitions.get(src, {})
        return state_transitions.get(input, self.defaults.get(src, None))

    def next_valid_string(self, input):
        state = self.start_state
        stack = []

        # Evaluate the DFA as far as possible
        for i, x in enumerate(input):
            stack.append((input[:i], state, x))
            state = self.next_state(state, x)
            if not state:
                break
        else:
            stack.append((input[:i + 1], state, None))

        if self.is_final(state):
            # Input word is already valid
            return input

        # Perform a 'wall following' search for the lexicographically smallest
        # accepting state.
        while stack:
            path, state, x = stack.pop()
            x = self.find_next_edge(state, x)
            if x:
                path += x
                state = self.next_state(state, x)
                if self.is_final(state):
                    return path
                stack.append((path, state, None))
        return None

    def find_next_edge(self, s, x):
        if x is None:
            x = u'\0'
        else:
            x = unichr(ord(x) + 1)
        state_transitions = self.transitions.get(s, {})
        if x in state_transitions or s in self.defaults:
            return x
        labels = sorted(state_transitions.keys())
        pos = bisect.bisect_left(labels, x)
        if pos < len(labels):
            return labels[pos]
        return None


def levenshtein_automata(term):
    k = 2

    nfa = NFA((0, 0))
    for i, c in enumerate(term):
        for e in range(k + 1):
            # Correct character
            nfa.add_transition((i, e), c, (i + 1, e))
            if e < k:
                # Deletion
                nfa.add_transition((i, e), NFA.ANY, (i, e + 1))
                # Insertion
                nfa.add_transition((i, e), NFA.EPSILON, (i + 1, e + 1))
                # Substitution
                nfa.add_transition((i, e), NFA.ANY, (i + 1, e + 1))
    for e in range(k + 1):
        if e < k:
            nfa.add_transition((len(term), e), NFA.ANY, (len(term), e + 1))
        nfa.add_final_state((len(term), e))
    return nfa


class Word:
    def __init__(self, stem, suffix):
        self.stem = stem
        self.suffix = suffix


class LatinStemmer:
    __que_exceptions = {
        "atque", "quoque", "neque", "itaque", "absque", "apsque", "abusque", "adaeque", "adusque",
        "denique", "deque", "susque", "oblique", "peraeque", "plenisque", "quandoque", "quisque",
        "quaeque", "cuiusque", "cuique", "quemque", "quamque", "quaque", "quique", "quorumque",
        "quarumque", "quibusque", "quosque", "quasque", "quotusquisque", "quousque", "ubique",
        "undique", "usque", "uterque", "utique", "utroque", "utribique", "torque", "coque",
        "concoque", "contorque", "detorque", "decoque", "excoque", "extorque", "obtorque", "optorque",
        "retorque", "recoque", "attorque", "incoque", "intorque", "praetorque"
    }

    __noun_suffixes = [
        "ibus", "ius",
        "ae", "am", "as", "em", "es", "ia", "is", "nt", "os", "ud", "um", "us",
        "a", "e", "i", "o", "u"
    ]

    def __init__(self):
        pass

    @staticmethod
    def stemmize(word):
        word = word.replace('j', 'i').replace('v', 'u')

        if word.endswith('que'):
            if word in LatinStemmer.__que_exceptions:
                return Word(stem=word, suffix='')
            word = word[:-3]

        for noun_suffix in LatinStemmer.__noun_suffixes:
            if word.endswith(noun_suffix):
                if len(word) - len(noun_suffix) >= 2:
                    return Word(stem=word[:-len(noun_suffix)], suffix=noun_suffix)
                else:
                    return Word(stem=word, suffix='')

        return Word(stem=word, suffix='')


class Matcher(object):
    def __init__(self, l):
        self.l = l
        self.probes = 0

    def __call__(self, w):
        self.probes += 1
        pos = bisect.bisect_left(self.l, w)
        if pos < len(self.l):
            return self.l[pos]
        else:
            return None


class Finder:
    def __init__(self, words_datasources):
        self.stem_to_words = {}
        self.words_to_datasources = words_datasources
        words_stems = []
        for idx, (word, data_sources) in enumerate(words_datasources.iteritems()):
            if idx % 100000 == 0:
                print(idx)

            word_stemmized = self.stemmize_word(word)

            stem_words_set = self.stem_to_words.get(word_stemmized, set())
            stem_words_set.add(word)
            self.stem_to_words[word_stemmized] = stem_words_set

            words_stems.append(word_stemmized)

        self.words = list(words_datasources.keys())
        self.words.sort()
        self.words = Matcher(self.words)

        words_stems.sort()
        self.words_stems = Matcher(words_stems)

    def stemmize_word(self, word):
        word_parts = word.split(' ')
        word_stemmized = ' '.join(LatinStemmer.stemmize(word_part).stem for word_part in word_parts)
        return word_stemmized

    def __find_all_matches(self, lev, lookup_func, lookup_ds):
        match = lev.next_valid_string(u'\0')
        while match:
            nxt = lookup_func(match)
            if not nxt:
                return
            if match == nxt:
                if lookup_ds(match):
                    yield match
                nxt = nxt + u'\0'
            match = lev.next_valid_string(nxt)

    def __match_by_stem(self, word, data_sources):
        word_stem = self.stemmize_word(word)
        lev = levenshtein_automata(word_stem).to_dfa()

        def lookup_ds(stem):
            if len(data_sources) == 0:
                return True
            s = set(
                ds
                for w in self.stem_to_words[stem]
                for ds in self.words_to_datasources[w]
            )
            intersect = s.intersection(data_sources)
            return len(intersect) > 0

        res = list(self.__find_all_matches(lev, self.words_stems, lookup_ds))
        return res

    def __match_by_full(self, word, data_sources):
        lev = levenshtein_automata(word).to_dfa()

        def lookup_ds(w):
            if len(data_sources) == 0:
                return True
            s = set(ds for ds in self.words_to_datasources[w])
            intersect = s.intersection(data_sources)
            return len(intersect) > 0

        res = list(self.__find_all_matches(lev, self.words, lookup_ds))
        return res

    def find_all_matches(self, word, data_sources=set()):
        word = re.sub('\s+', ' ', word.strip()).lower()
        print(word)

        if ' ' not in word:
            print('single word match')
            if word in self.words_to_datasources:
                res = [word]
            else:
                res = []

        else:
            matches_by_stem = self.__match_by_stem(word, data_sources)
            if len(matches_by_stem) > 0:
                res = [
                    w
                    for match_by_stem in matches_by_stem
                    for w in self.stem_to_words[match_by_stem]
                ]

                if len(data_sources) > 0:
                    res = [r for r in res
                           if len(data_sources.intersection(self.words_to_datasources[r]))]

            else:
                res = self.__match_by_full(word, data_sources)

                if len(data_sources) > 0:
                    res = [r for r in res
                           if len(data_sources.intersection(self.words_to_datasources[r]))]

        res = [r[0].upper() + r[1:] for r in res]

        print 'res:', res
        return res
