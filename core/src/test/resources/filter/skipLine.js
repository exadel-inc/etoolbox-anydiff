function skipLoremIpsum(line) {
    return line.getLeft().includes('Lorem ipsum') && line.getRight().includes('Dolor sit amet');
}

function skipAnalytics(line) {
    return line.getRight().trim().startsWith('data-analytics');
}