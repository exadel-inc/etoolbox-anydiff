function skip(diff) {
    console.log(diff.getState());
    return String(diff.getState()) === 'CHANGE';
}
