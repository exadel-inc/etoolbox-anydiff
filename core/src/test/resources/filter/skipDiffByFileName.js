function skip(diff) {
    console.log('left', diff.getLeft(), 'right', diff.getRight());
    return diff.getLeft().endsWith(".html") && diff.getRight().endsWith(".html")
}
