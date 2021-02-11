# Compiler - Alpha

Compiles to simplified assembly used by virtual machine with 6 registers a-f.

## Grammar
- Program structure
    - With declaration
    ```c
    DECLARE
   	    a, b, t(10:20)
    BEGIN
        ...
    END
    ```
	- Without declaration
    ```c
    BEGIN
        ...
    END
    ```
- Comments
    ```c
    [ OK ]
    [ Also
    OK ]
    [ so [ wrong ]  ]
    ```
* Variables
    * Ordinary - [_a-z]+
    ```c
    a   [ ok ]
    _a  [ ok ]
    A1 [ Wrong ]
  ```
    * Array - t(x:y), where x <= y
    ```c
    tab(10:20) [ declaration of array of size=11 indexed from 10 to 20 ]
    tab(15) [ Ok ]
    tab(21) [ Index out of bounds]
    tab(a) [ if 10 <= a <= 20 Ok ]
    ``` 
    * Constants - only integers >= 0
    ```c
    a := 0; [ Ok ]
    a := 1234567890987654321; [ Ok ]
    a / 0 = 0;
    a % 0 = 0;
    ```
- IO
    ```c
    READ a;  		[ Read from stdin to variable 'a' ]
    READ 123; 		[ Wrong ]
    WRITE a;		[ Write variable 'a' to stdout ]
    WRITE tab(a);		[ Write array variable to stdout ]
    WRITE 123;		[ Write constant to stdout ]
    ```
- Expressions and conditions
    ```c
    [ Expressions ]
    a := 10;
    a := b;
    a := b * c; [+ - * % /]
    a := b * c * d; [ Wrong ]
    a := b * c; [ Only expressions with single operator are allowed ]
    a := a * d;
    [ Conditions ]
    a = b [ Equals ]
    a != b [ Not equal ]
    a < b [ Less ]
    a > b [ Greater ]
    a <= b [ Less equal ]
    a >= b [ Greater equal ]
    ```

- If  
    TBA
- Loops  
    TBA