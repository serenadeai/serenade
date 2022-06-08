#!/bin/bash
set -exu

kaldi_dir=$(realpath $1)
lm=$(realpath $2)
prefix=$3
output=$4
words=$prefix/new/graph/words.txt

cd $kaldi_dir/egs/librispeech/s5/

. cmd.sh
. path.sh

echo "Creating ConstArpa for rescoring..."
utils/map_arpa_lm.pl $words < $lm > $prefix/tmp/lm.int
arpa-to-const-arpa \
    --bos-symbol=`grep "<s>" $words | cut -d' ' -f2` \
    --eos-symbol=`grep "</s>" $words | cut -d' ' -f2` \
    $prefix/tmp/lm.int $prefix/new/$output
