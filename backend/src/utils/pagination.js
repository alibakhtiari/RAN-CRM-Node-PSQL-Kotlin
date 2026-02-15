const getPaginationParams = (req) => {
  const page = parseInt(req.query.page) || 1;
  const rawLimit = parseInt(req.query.limit) || 10;
  const limit = Math.min(Math.max(rawLimit, 1), 100);
  const offset = (page - 1) * limit;

  return { page, limit, offset };
};

const getPaginationResult = (results, total, page, limit) => {
  const totalPages = Math.ceil(total / limit);

  return {
    data: results,
    pagination: {
      currentPage: page,
      totalPages,
      totalItems: total,
      itemsPerPage: limit,
      hasNext: page < totalPages,
      hasPrev: page > 1,
    },
  };
};

module.exports = { getPaginationParams, getPaginationResult };
