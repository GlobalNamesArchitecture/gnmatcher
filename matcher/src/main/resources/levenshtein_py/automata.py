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


def _stemmize_word(word):
    word_parts = word.split(' ')
    word_stemmized = ' '.join(LatinStemmer.stemmize(word_part).stem for word_part in word_parts)
    return word_stemmized


def _find_all_matches(lev, lookup_func, lookup_ds):
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


class MatcherByStem:
    def __init__(self, words_to_datasources):
        print("Constructing MatcherByStem")

        self.word_stemmized_to_words = {}
        self.words_to_datasources = words_to_datasources

        for idx, (word, data_sources) in enumerate(words_to_datasources.iteritems()):
            if idx > 0 and idx % 100000 == 0:
                print word, data_sources
                print(idx)

            word_stemmized = self.transform(word)
            if word_stemmized not in self.word_stemmized_to_words:
                self.word_stemmized_to_words[word_stemmized] = set()
            self.word_stemmized_to_words[word_stemmized].add(word)

        word_stemmized = list(self.word_stemmized_to_words.keys())
        word_stemmized.sort()
        self.bisect_by_word_stems = Matcher(word_stemmized)
        pass

    def match(self, word, data_sources):
        word_stem = self.transform(word)
        lev = levenshtein_automata(word_stem).to_dfa()

        def lookup_ds(stem):
            if len(data_sources) == 0:
                return True
            s = set(
                ds
                for w in self.word_stemmized_to_words[stem]
                for ds in self.words_to_datasources[w]
            )
            intersect = s.intersection(data_sources)
            return len(intersect) > 0

        res = list(_find_all_matches(lev, self.bisect_by_word_stems, lookup_ds))
        return res

    @staticmethod
    def transform(word):
        word_stemmized = _stemmize_word(word.lower())
        return word_stemmized

    def lookup(self, word_transformed):
        return self.word_stemmized_to_words.get(word_transformed, set())


class MatcherByVerbatim:
    def __init__(self, words_to_datasources):
        print("Constructing MatcherByVerbatim")

        self.words_to_datasources = words_to_datasources
        self.words_verbatims_to_words = {}

        for idx, word in enumerate(words_to_datasources.keys()):
            if idx > 0 and idx % 100000 == 0:
                print(idx)

            word_verbatim = self.transform(word)

            if word_verbatim not in self.words_verbatims_to_words:
                self.words_verbatims_to_words[word_verbatim] = set()
            self.words_verbatims_to_words[word_verbatim].add(word)

        word_verbatims = list(self.words_verbatims_to_words.keys())
        word_verbatims.sort()
        self.bisect_by_word_verbatims = Matcher(word_verbatims)
        pass

    def match(self, word, data_sources):
        word_verbatim = self.transform(word)
        lev = levenshtein_automata(word_verbatim).to_dfa()

        def lookup_ds(w_verb):
            if len(data_sources) == 0:
                return True
            s = set(ds
                    for w_orig in self.words_verbatims_to_words[w_verb]
                    for ds in self.words_to_datasources[w_orig])
            intersect = s.intersection(data_sources)
            return len(intersect) > 0

        res = list(_find_all_matches(lev, self.bisect_by_word_verbatims, lookup_ds))
        return res

    @staticmethod
    def transform(word):
        word_verbatim = word.lower().replace('j', 'i').replace('v', 'u')
        return word_verbatim

    def lookup(self, word_transformed):
        return self.words_verbatims_to_words.get(word_transformed, set())


class MatcherByGenusOnly:
    def __init__(self, words_to_datasources):
        print("Constructing MatcherByGenusOnly")

        self.words_to_datasources = words_to_datasources
        self.words_genus_only_to_words = {}

        for idx, word in enumerate(words_to_datasources.keys()):
            if idx > 0 and idx % 100000 == 0:
                print(idx)

            word_transformed = self.transform(word)
            if ' ' in word_transformed:
                continue

            if word_transformed not in self.words_genus_only_to_words:
                self.words_genus_only_to_words[word_transformed] = set()
            self.words_genus_only_to_words[word_transformed].add(word)

        pass

    def match(self, word, data_sources):
        word_transformed = self.transform(word)
        res = self.words_genus_only_to_words.get(word_transformed, set())
        return [r.lower() for r in res]

    @staticmethod
    def transform(word):
        word_verbatim = word.lower().replace('j', 'i').replace('v', 'u')
        return word_verbatim

    def lookup(self, word_transformed):
        res = self.words_genus_only_to_words.get(word_transformed, set())
        return res


class Finder:
    def __init__(self, words_to_datasources):
        self.words_to_datasources = words_to_datasources
        self.matcher_by_stem = MatcherByStem(words_to_datasources)
        self.matcher_by_verbatim = MatcherByVerbatim(words_to_datasources)
        self.matcher_by_genus_only = MatcherByGenusOnly(words_to_datasources)

    def __pipeline(self, word, data_sources=set()):
        word_cleaned = re.sub('\s+', ' ', word.strip()).lower()
        print 'request: ', word_cleaned, '|', data_sources

        matches_genus_only = self.matcher_by_genus_only.match(word_cleaned, data_sources)
        if matches_genus_only:
            print 'single word match', matches_genus_only
            res = [
                w
                for match_genus_only in matches_genus_only
                for w in self.matcher_by_genus_only.lookup(match_genus_only)
            ]
            print 'single word match (filtered)', res
            return res

        else:
            matches_by_stem = self.matcher_by_stem.match(word_cleaned, data_sources)
            print 'matches_by_stem', matches_by_stem
            if matches_by_stem:
                res = [
                    w
                    for match_by_stem in matches_by_stem
                    for w in self.matcher_by_stem.lookup(match_by_stem)
                ]

                if data_sources:
                    res = [r for r in res
                           if len(data_sources.intersection(self.words_to_datasources[r]))]
                    print 'matches_by_stem (filtered)', res

            else:
                matches_by_verbatim = self.matcher_by_verbatim.match(word_cleaned, data_sources)
                res = [
                    w
                    for match_by_verbatim in matches_by_verbatim
                    for w in self.matcher_by_verbatim.lookup(match_by_verbatim)
                ]
                print 'matches_by_verbatim', matches_by_verbatim

                if data_sources:
                    res = [r for r in res
                           if len(data_sources.intersection(self.words_to_datasources[r]))]
                    print 'matches_by_verbatim (filtered)', res

        print 'res:', res
        return res

    def find_all_matches(self, word, data_sources=set()):
        import traceback
        try:
            return self.__pipeline(word, data_sources)
        except Exception as ex:
            print ex
            traceback.print_exc()
            return []
