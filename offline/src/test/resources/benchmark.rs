fn factorial(number: u32) -> u32 {
    if number == 0 {
        return 1;
    } else {
        return number * factorial(number - 1);
    }
}
