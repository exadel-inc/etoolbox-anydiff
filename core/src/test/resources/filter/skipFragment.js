function skip(fragment) {
    return fragment.isTagContent('h1')
        || fragment.isAttributeValue('class')
        || String(fragment).includes('nunc consequat interdum');
}