## Spinal-bootcamp

There have two branch 
- **en** as default (merge to master) 
- **zh-cn** for 中文 users (You need `git checkout zh-cn` first)

## Setup Jupyter-notebook enviroment 

install follows first 
- [jupyter-notebook](https://jupyter.org/install)
- scala(at least 2.12)
- [almond](https://almond.sh/)(scala kernel for jupyter) 

## Usage

```shell
$: git clone https://github.com/jijingg/Spinal-bootcamp
$: cd Spinal-bootcamp
$: jupyter notebook &
```

## Tree

Bus
- apb
- axi 
- ahb

Structur
- component
- area
- clockDomain

Operation
- +-/* /*
- << >>
- why function

FixPoint
- UInt/SInt
- fixed

## creat Slide
`jupyter-nbconvert --to slides spinal_21_example_filter.ipynb --reveal-prefix  'https://cdn.bootcss.com/reveal.js/3.5.0' --output test`
