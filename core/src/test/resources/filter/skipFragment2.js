function accept(fragment) {
    return fragment.isAttributeValue('tag')
        || fragment.isTagContent('title')
        || String(fragment).includes('ranking')
        || fragment.isTagContent('ranking');
}