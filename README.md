## Spinal-bootcamp

There have two branch 
- **en** as default (merge to master) 
- **zh-cn** for 中文 users (You need `git checkout zh-cn` first)

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
` jupyter-nbconvert --to slides spinal_21_example_filter.ipynb --reveal-prefix  'https://cdn.bootcss.com/reveal.js/3.5.0' --output test`
