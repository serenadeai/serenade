#!/bin/bash
set -exu

kaldi_dir=$(realpath $1)
prefix=$2
lm=$(realpath $3)
lexicon=$(realpath $4)
scripts_dir=$(realpath $5)
acoustic_model=$6
cd $kaldi_dir/egs/librispeech/s5/

. cmd.sh
. path.sh

export LC_ALL=C

rm -rf $prefix/data/lang
rm -rf $prefix/data/local/lang
rm -rf $prefix/data/local/dict

mkdir -p $prefix/data/local/dict
cp $scripts_dir/dict/* $prefix/data/local/dict/
cp $lexicon $prefix/data/local/dict/
cp $lm $prefix/lm.arpa
echo "#nonterm:hint" > $prefix/data/local/dict/nonterminals.txt

utils/prepare_lang.sh $prefix/data/local/dict "nspc" $prefix/data/local/lang $prefix/data/lang

# Rerun the last command of prepare_lang with a modification for spelling silence.
cat $scripts_dir/../spelling/lexiconp_disambig.txt >> $prefix/data/local/lang/lexiconp_silprob_disambig.txt;
ndisambig=$(cat $prefix/data/local/lang/lex_ndisambig)
utils/lang/make_lexicon_fst.py --left-context-phones=$prefix/data/lang/phones/left_context_phones.txt \
    --nonterminals=$prefix/data/local/dict/nonterminals.txt \
    --sil-prob=0.5 --sil-phone=SIL --sil-disambig='#'$ndisambig $prefix/data/local/lang/lexiconp_disambig.txt | \
  fstcompile --isymbols=$prefix/data/lang/phones.txt --osymbols=$prefix/data/lang/words.txt --keep_isymbols=false \
    --keep_osymbols=false | \
  fstaddselfloops  $prefix/data/lang/phones/wdisambig_phones.int $prefix/data/lang/phones/wdisambig_words.int | \
  fstarcsort --sort_type=olabel > $prefix/data/lang/L_disambig.fst

echo "Creating G.fst..."

cat $prefix/lm.arpa | utils/find_arpa_oovs.pl $prefix/data/lang/words.txt > $prefix/oovs_lm.txt
nonterm_hint=$(grep '#nonterm:hint' $prefix/data/lang/words.txt | awk '{print $2}')

cat $prefix/lm.arpa | \
grep -v '<s> <s>' | \
grep -v '</s> <s>' | \
grep -v '</s> </s>' | \
arpa2fst - | fstprint | \
utils/remove_oovs.pl $prefix/oovs_lm.txt | \
utils/eps2disambig.pl | utils/s2eps.pl | fstcompile --isymbols=$prefix/data/lang/words.txt \
  --osymbols=$prefix/data/lang/words.txt  --keep_isymbols=false --keep_osymbols=false | \
 fstrmepsilon | \
 fstrmsymbols --remove-from-output=true "echo $nonterm_hint|" - $prefix/data/lang/G.fst

mkdir -p $prefix/tmp/exp
cp -r $acoustic_model/exp $prefix/tmp/
utils/mkgraph.sh $prefix/data/lang $prefix/tmp/exp/chain_cleaned/tdnn_1d_aug $prefix/new/graph

echo "Creating decodable configuration..."
cp -r $acoustic_model/conf $prefix/tmp/
steps/online/nnet3/prepare_online_decoding.sh --mfcc-config $prefix/tmp/conf/mfcc_hires.conf $prefix/data/lang $prefix/tmp/exp/nnet3_cleaned/extractor $prefix/tmp/exp/chain_cleaned/tdnn_1d_aug $prefix/new
